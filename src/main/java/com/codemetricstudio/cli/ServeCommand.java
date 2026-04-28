package com.codemetricstudio.cli;

import com.codemetricstudio.model.ProjectMetrics;
import com.codemetricstudio.service.ProjectAnalysisService;
import com.codemetricstudio.service.ProjectAnalysisService.UploadedJavaFile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

@CommandLine.Command(name = "serve", mixinStandardHelpOptions = true, description = "Start local HTTP API for frontend uploads")
public class ServeCommand implements Callable<Integer> {
    private static final String BUILTIN_SILICONFLOW_API_KEY = "sk-vpvxkfywbqivcyburueivnmzgazvoessctcsbzjisjtnpjpa";
    private static final String DEFAULT_SILICONFLOW_MODEL = "deepseek-ai/DeepSeek-V3.1-Terminus";
    private static final String DEFAULT_SILICONFLOW_BASE_URL = "https://api.siliconflow.cn/v1/chat/completions";

    @CommandLine.Option(names = "--port", description = "HTTP server port", defaultValue = "9090")
    private int port;

    @CommandLine.Option(names = "--threshold", description = "Threshold config JSON file")
    private Path thresholdPath;

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final ProjectAnalysisService analysisService = new ProjectAnalysisService();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    @Override
    public Integer call() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/health", new JsonHandler(this::health));
        server.createContext("/api/analyze-project", new JsonHandler(this::analyzeProject));
        server.createContext("/api/ai-analysis", new JsonHandler(this::aiAnalysis));
        server.createContext("/", new StaticFileHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("CodeMetric Studio upload API started at http://127.0.0.1:" + port);
        System.out.println("Open frontend at http://127.0.0.1:" + port + "/web/");
        System.out.println("Use Ctrl+C to stop the server.");
        Thread.currentThread().join();
        return 0;
    }

    private ApiResponse health(HttpExchange exchange) {
        return ApiResponse.ok(Map.of("status", "ok"));
    }

    private ApiResponse analyzeProject(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            return ApiResponse.error(405, "Method not allowed");
        }

        try (InputStream body = exchange.getRequestBody()) {
            AnalyzeProjectRequest request = mapper.readValue(body, AnalyzeProjectRequest.class);
            if (request == null || request.files == null || request.files.isEmpty()) {
                return ApiResponse.error(400, "No Java project files were uploaded.");
            }

            List<UploadedJavaFile> uploadedFiles = new ArrayList<>();
            for (UploadedFilePayload file : request.files) {
                if (file == null || file.relativePath == null || !file.relativePath.endsWith(".java")) {
                    continue;
                }
                uploadedFiles.add(new UploadedJavaFile(file.relativePath, file.content));
            }
            if (uploadedFiles.isEmpty()) {
                return ApiResponse.error(400, "Uploaded folder does not contain .java files.");
            }

            ProjectMetrics metrics = analysisService.analyzeUploadedProject(request.projectName, uploadedFiles, thresholdPath);
            return ApiResponse.ok(metrics);
        } catch (IllegalArgumentException ex) {
            return ApiResponse.error(400, ex.getMessage());
        } catch (Exception ex) {
            return ApiResponse.error(500, "Analysis failed: " + ex.getMessage());
        }
    }

    private ApiResponse aiAnalysis(HttpExchange exchange) throws IOException, InterruptedException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            return ApiResponse.error(405, "Method not allowed");
        }

        String apiKey = readConfig("SILICONFLOW_API_KEY", BUILTIN_SILICONFLOW_API_KEY);
        if (apiKey.isBlank()) {
            return ApiResponse.error(400, "Missing SiliconFlow API key.");
        }

        AiAnalysisRequest request;
        try (InputStream body = exchange.getRequestBody()) {
            request = mapper.readValue(body, AiAnalysisRequest.class);
        }
        if (request == null || request.context == null) {
            return ApiResponse.error(400, "Missing analysis context.");
        }

        String model = readConfig("SILICONFLOW_MODEL", DEFAULT_SILICONFLOW_MODEL);
        String endpoint = readConfig("SILICONFLOW_BASE_URL", DEFAULT_SILICONFLOW_BASE_URL);
        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", model);
        payload.put("temperature", 0.25);
        payload.put("max_tokens", 1600);

        ArrayNode messages = payload.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", """
                        你是资深软件架构与软件度量分析助手。请结合用户问题和当前软件度量数据，判断当前类图设计、软件项目或顺序图的设计质量。
                        输出中文 Markdown，必须包含：
                        1. 总体结论，给出“良好/一般/较差”之一和简短原因。
                        2. 关键证据，引用具体度量值或图结构数据。
                        3. 主要风险，按影响排序。
                        4. 改进建议，尽量可执行。
                        不要编造没有出现在上下文中的类名、方法名或指标。
                        """);
        messages.addObject()
                .put("role", "user")
                .put("content", buildAiPrompt(request));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        JsonNode responseJson = parseJsonSafely(response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message = responseJson.path("error").path("message").asText();
            if (message.isBlank()) {
                message = "SiliconFlow request failed with HTTP " + response.statusCode();
            }
            return ApiResponse.error(response.statusCode(), message);
        }

        String content = responseJson.path("choices").path(0).path("message").path("content").asText();
        if (content.isBlank()) {
            return ApiResponse.error(502, "SiliconFlow returned an empty analysis result.");
        }

        return ApiResponse.ok(Map.of(
                "model", model,
                "content", content
        ));
    }

    private JsonNode parseJsonSafely(String body) {
        try {
            return mapper.readTree(body == null ? "" : body);
        } catch (Exception ex) {
            ObjectNode fallback = mapper.createObjectNode();
            fallback.put("raw", body == null ? "" : body);
            return fallback;
        }
    }

    private String buildAiPrompt(AiAnalysisRequest request) throws IOException {
        String userInput = defaultIfBlank(request.userInput, "请评价当前设计质量，并指出最值得优先修改的地方。");
        String contextJson = mapper.writeValueAsString(request.context);
        return """
                用户关注点：
                %s

                当前度量上下文 JSON：
                %s
                """.formatted(userInput, contextJson);
    }

    private String readConfig(String name, String fallback) {
        return defaultIfBlank(readEnvOrDotEnv(name), fallback);
    }

    private String readEnvOrDotEnv(String name) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return readDotEnv(name);
    }

    private String readDotEnv(String name) {
        Path dotEnv = Paths.get(".env");
        if (!Files.isRegularFile(dotEnv)) {
            return "";
        }
        try {
            for (String line : Files.readAllLines(dotEnv)) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#")) {
                    continue;
                }
                int split = trimmed.indexOf('=');
                if (split <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, split).trim();
                if (!name.equals(key)) {
                    continue;
                }
                return stripEnvQuotes(trimmed.substring(split + 1).trim());
            }
        } catch (IOException ignored) {
            return "";
        }
        return "";
    }

    private String stripEnvQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private final class JsonHandler implements HttpHandler {
        private final ApiAction action;

        private JsonHandler(ApiAction action) {
            this.action = action;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange.getResponseHeaders());
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            ApiResponse response;
            try {
                response = action.handle(exchange);
            } catch (Exception ex) {
                response = ApiResponse.error(500, "Unhandled server error: " + ex.getMessage());
            }

            byte[] bytes = mapper.writeValueAsBytes(response.body);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(response.status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private void addCorsHeaders(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private final class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange.getResponseHeaders());
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }

            String path = URLDecoder.decode(exchange.getRequestURI().getPath(), StandardCharsets.UTF_8);
            if ("/".equals(path)) {
                exchange.getResponseHeaders().set("Location", "/web/");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }
            if ("/web".equals(path)) {
                exchange.getResponseHeaders().set("Location", "/web/");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }
            if ("/web/".equals(path)) {
                path = "/web/index.html";
            }
            if (!path.startsWith("/web/") && !path.startsWith("/out/")) {
                sendText(exchange, 404, "Not found");
                return;
            }

            Path root = Paths.get(".").toAbsolutePath().normalize();
            Path file = root.resolve(path.substring(1)).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                sendText(exchange, 404, "Not found");
                return;
            }

            byte[] bytes = Files.readAllBytes(file);
            exchange.getResponseHeaders().set("Content-Type", contentType(file));
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, "HEAD".equalsIgnoreCase(exchange.getRequestMethod()) ? -1 : bytes.length);
            if (!"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                exchange.close();
            }
        }

        private String contentType(Path file) {
            String name = file.getFileName().toString().toLowerCase();
            if (name.endsWith(".html")) return "text/html; charset=UTF-8";
            if (name.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (name.endsWith(".css")) return "text/css; charset=UTF-8";
            if (name.endsWith(".json")) return "application/json; charset=UTF-8";
            if (name.endsWith(".svg")) return "image/svg+xml";
            if (name.endsWith(".png")) return "image/png";
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
            return "application/octet-stream";
        }
    }

    private void sendText(HttpExchange exchange, int status, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private record ApiResponse(int status, Object body) {
        private static ApiResponse ok(Object body) {
            return new ApiResponse(200, body);
        }

        private static ApiResponse error(int status, String message) {
            return new ApiResponse(status, Map.of("error", message));
        }
    }

    @FunctionalInterface
    private interface ApiAction {
        ApiResponse handle(HttpExchange exchange) throws Exception;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AnalyzeProjectRequest {
        public String projectName;
        public List<UploadedFilePayload> files;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UploadedFilePayload {
        public String relativePath;
        public String content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AiAnalysisRequest {
        public String userInput;
        public JsonNode context;
    }
}
