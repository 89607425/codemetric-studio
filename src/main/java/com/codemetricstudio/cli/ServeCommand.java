package com.codemetricstudio.cli;

import com.codemetricstudio.model.ProjectMetrics;
import com.codemetricstudio.service.ProjectAnalysisService;
import com.codemetricstudio.service.ProjectAnalysisService.UploadedJavaFile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

@CommandLine.Command(name = "serve", mixinStandardHelpOptions = true, description = "Start local HTTP API for frontend uploads")
public class ServeCommand implements Callable<Integer> {

    @CommandLine.Option(names = "--port", description = "HTTP server port", defaultValue = "9090")
    private int port;

    @CommandLine.Option(names = "--threshold", description = "Threshold config JSON file")
    private Path thresholdPath;

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final ProjectAnalysisService analysisService = new ProjectAnalysisService();

    @Override
    public Integer call() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/health", new JsonHandler(this::health));
        server.createContext("/api/analyze-project", new JsonHandler(this::analyzeProject));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("CodeMetric Studio upload API started at http://127.0.0.1:" + port);
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
}
