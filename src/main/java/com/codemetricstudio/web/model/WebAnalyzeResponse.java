package com.codemetricstudio.web.model;

import com.codemetricstudio.model.ProjectMetrics;

public class WebAnalyzeResponse {
    private ProjectMetrics projectMetrics;
    private DesignMetricsResult designMetrics;
    private String markdownReport;
    private long elapsedMs;

    public ProjectMetrics getProjectMetrics() {
        return projectMetrics;
    }

    public void setProjectMetrics(ProjectMetrics projectMetrics) {
        this.projectMetrics = projectMetrics;
    }

    public DesignMetricsResult getDesignMetrics() {
        return designMetrics;
    }

    public void setDesignMetrics(DesignMetricsResult designMetrics) {
        this.designMetrics = designMetrics;
    }

    public String getMarkdownReport() {
        return markdownReport;
    }

    public void setMarkdownReport(String markdownReport) {
        this.markdownReport = markdownReport;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }
}
