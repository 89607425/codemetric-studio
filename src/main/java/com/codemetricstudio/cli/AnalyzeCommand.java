package com.codemetricstudio.cli;

import com.codemetricstudio.model.ProjectMetrics;
import com.codemetricstudio.reporter.JsonReporter;
import com.codemetricstudio.reporter.MarkdownReporter;
import com.codemetricstudio.service.ProjectAnalysisService;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "analyze", mixinStandardHelpOptions = true, description = "Analyze Java project metrics")
public class AnalyzeCommand implements Callable<Integer> {

    @CommandLine.Option(names = "--path", required = true, description = "Java project root path")
    private Path projectPath;

    @CommandLine.Option(names = "--out", description = "Output directory", defaultValue = "./out")
    private Path outputDir;

    @CommandLine.Option(names = "--format", description = "Output format: ${COMPLETION-CANDIDATES}", defaultValue = "all")
    private OutputFormat format;

    @CommandLine.Option(names = "--module", description = "Analyze a sub directory only")
    private String module;

    @CommandLine.Option(names = "--threshold", description = "Threshold config JSON file")
    private Path thresholdPath;

    @Override
    public Integer call() {
        long start = System.currentTimeMillis();
        try {
            ProjectMetrics metrics = new ProjectAnalysisService().analyzePath(projectPath, module, thresholdPath);

            if (format == OutputFormat.json || format == OutputFormat.all) {
                new JsonReporter().write(metrics, outputDir);
            }
            if (format == OutputFormat.md || format == OutputFormat.all) {
                new MarkdownReporter().write(metrics, outputDir);
            }

            long elapsed = System.currentTimeMillis() - start;
            printSummary(metrics, elapsed);
            return 0;
        } catch (Exception ex) {
            System.err.println("Analysis failed: " + ex.getMessage());
            return 1;
        }
    }

    private void printSummary(ProjectMetrics metrics, long elapsedMs) {
        double avgComplexity = metrics.getMethods().isEmpty()
                ? 0
                : metrics.getMethods().stream().mapToInt(m -> m.getCyclomaticComplexity()).average().orElse(0);

        System.out.println("=== CodeMetric Studio Summary ===");
        System.out.println("Project: " + metrics.getProjectName());
        System.out.println("Files: " + metrics.getFileCount());
        System.out.println("Classes: " + metrics.getClassCount());
        System.out.println("Methods: " + metrics.getMethodCount());
        System.out.println("Average Cyclomatic Complexity: " + String.format("%.2f", avgComplexity));
        System.out.println("Alerts: " + metrics.getAlerts().size());
        System.out.println("Elapsed: " + elapsedMs + " ms");
        System.out.println("Output Directory: " + outputDir.toAbsolutePath());
    }
}
