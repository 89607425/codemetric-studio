package com.codemetricstudio.service;

import com.codemetricstudio.aggregator.MetricsAggregator;
import com.codemetricstudio.config.ThresholdConfig;
import com.codemetricstudio.config.ThresholdConfigLoader;
import com.codemetricstudio.metrics.ProjectAnalyzer;
import com.codemetricstudio.model.ProjectMetrics;
import com.codemetricstudio.scanner.ProjectScanner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class ProjectAnalysisService {

    public ProjectMetrics analyzePath(Path projectPath, String module, Path thresholdPath) throws IOException {
        ProjectScanner scanner = new ProjectScanner();
        List<Path> files = scanner.scan(projectPath, module);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("No Java files found under path: " + projectPath);
        }

        ThresholdConfig config = ThresholdConfigLoader.load(thresholdPath);
        String projectName = projectPath.getFileName() == null ? projectPath.toString() : projectPath.getFileName().toString();
        ProjectAnalyzer analyzer = new ProjectAnalyzer();
        MetricsAggregator aggregator = new MetricsAggregator();
        return aggregator.aggregate(analyzer.analyze(projectName, files), config);
    }

    public ProjectMetrics analyzeUploadedProject(String projectName, List<UploadedJavaFile> files, Path thresholdPath) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No uploaded Java files were provided.");
        }

        Path tempRoot = Files.createTempDirectory("codemetric-upload-");
        try {
            for (UploadedJavaFile file : files) {
                if (file == null || file.relativePath() == null || file.relativePath().isBlank()) {
                    continue;
                }
                Path destination = tempRoot.resolve(file.relativePath()).normalize();
                if (!destination.startsWith(tempRoot)) {
                    throw new IllegalArgumentException("Invalid file path: " + file.relativePath());
                }
                Files.createDirectories(destination.getParent());
                Files.writeString(destination, file.content() == null ? "" : file.content(), StandardCharsets.UTF_8);
            }
            ProjectMetrics metrics = analyzePath(tempRoot, null, thresholdPath);
            if (projectName != null && !projectName.isBlank()) {
                metrics.setProjectName(projectName);
            }
            return metrics;
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    private void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignore) {
                    // Best-effort cleanup for temp upload directory.
                }
            });
        }
    }

    public record UploadedJavaFile(String relativePath, String content) {
    }
}
