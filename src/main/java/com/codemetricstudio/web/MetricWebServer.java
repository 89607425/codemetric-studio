package com.codemetricstudio.web;

import com.codemetricstudio.web.model.WebAnalyzeRequest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;

public class MetricWebServer {

    private final HttpServer server;
    private final ObjectMapper mapper;
    private final WebAnalysisService analysisService;
    private final Path webRoot;

    public MetricWebServer(String host, int port, Path webRoot) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(host, port), 0);
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.analysisService = new WebAnalysisService();
        this.webRoot = webRoot.toAbsolutePath().normalize();
        registerRoutes();
    }

    public void start() {
        server.setExecutor(Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors())));
        server.start();
    }

    public void stop(int delaySeconds) {
        server.stop(delaySeconds);
    }

    private void registerRoutes() {
        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/analyze", this::handleAnalyze);
        server.createContext("/", new StaticFileHandler(webRoot));
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        writeJson(exchange, 200, Map.of("status", "ok", "time", Instant.now().toString()));
    }

    private void handleAnalyze(HttpExchange exchange) throws IOException {
        if (isOptions(exchange)) {
            handleOptions(exchange);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            WebAnalyzeRequest request = mapper.readValue(body, WebAnalyzeRequest.class);
            if (request.getSourceFiles() == null || request.getSourceFiles().isEmpty()) {
                writeJson(exchange, 400, Map.of("error", "sourceFiles is required and cannot be empty"));
                return;
            }

            var response = analysisService.analyze(request);
            writeJson(exchange, 200, response);
        } catch (Exception ex) {
            writeJson(exchange, 500, Map.of("error", "analyze_failed", "message", ex.getMessage()));
        }
    }

    private boolean isOptions(HttpExchange exchange) {
        return "OPTIONS".equalsIgnoreCase(exchange.getRequestMethod());
    }

    private void handleOptions(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private void writeJson(HttpExchange exchange, int status, Object payload) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(payload);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static class StaticFileHandler implements HttpHandler {
        private final Path root;

        private StaticFileHandler(Path root) {
            this.root = root;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath == null || requestPath.equals("/")) {
                requestPath = "/index.html";
            }

            Path target = root.resolve(requestPath.substring(1)).normalize();
            if (!target.startsWith(root) || !Files.exists(target) || Files.isDirectory(target)) {
                byte[] notFound = "Not Found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, notFound.length);
                exchange.getResponseBody().write(notFound);
                exchange.close();
                return;
            }

            String mimeType = URLConnection.guessContentTypeFromName(target.getFileName().toString());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            byte[] content = Files.readAllBytes(target);
            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
            exchange.close();
        }
    }
}
