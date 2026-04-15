package com.codemetricstudio.reporter;

import com.codemetricstudio.model.Alert;
import com.codemetricstudio.model.ClassMetrics;
import com.codemetricstudio.model.MethodMetrics;
import com.codemetricstudio.model.ProjectMetrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class MarkdownReporter {

    public void write(ProjectMetrics metrics, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        Path out = outputDir.resolve("metrics-report.md");
        Files.writeString(out, build(metrics));
    }

    public String build(ProjectMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("# CodeMetric Studio Report\n\n");
        sb.append("## 项目概览\n\n");
        sb.append("- 项目名称: ").append(metrics.getProjectName()).append("\n");
        sb.append("- 文件数: ").append(metrics.getFileCount()).append("\n");
        sb.append("- 类数: ").append(metrics.getClassCount()).append("\n");
        sb.append("- 方法数: ").append(metrics.getMethodCount()).append("\n");
        sb.append("- 总行数: ").append(metrics.getLoc().getTotalLines()).append("\n");
        sb.append("- 有效代码行: ").append(metrics.getLoc().getCodeLines()).append("\n");
        sb.append("- 告警数: ").append(metrics.getAlerts().size()).append("\n\n");

        sb.append("## 指标汇总表\n\n");
        sb.append("| 类 | WMC | DIT | NOC | CBO | RFC | LCOM |\n");
        sb.append("|---|---:|---:|---:|---:|---:|---:|\n");
        for (ClassMetrics c : metrics.getClasses()) {
            sb.append("|").append(c.getClassName()).append("|")
                    .append(c.getWmc()).append("|")
                    .append(c.getDit()).append("|")
                    .append(c.getNoc()).append("|")
                    .append(c.getCbo()).append("|")
                    .append(c.getRfc()).append("|")
                    .append(String.format("%.2f", c.getLcom())).append("|\n");
        }
        sb.append("\n");

        sb.append("## 高风险类 Top10\n\n");
        sb.append("| 类 | 风险评分(WMC+CBO*2) |\n");
        sb.append("|---|---:|\n");
        metrics.getClasses().stream()
                .sorted(Comparator.comparingInt((ClassMetrics c) -> c.getWmc() + c.getCbo() * 2).reversed())
                .limit(10)
                .forEach(c -> sb.append("|").append(c.getClassName()).append("|")
                        .append(c.getWmc() + c.getCbo() * 2)
                        .append("|\n"));
        sb.append("\n");

        sb.append("## 高风险方法 Top10\n\n");
        sb.append("| 方法 | 圈复杂度 | LOC |\n");
        sb.append("|---|---:|---:|\n");
        metrics.getMethods().stream()
                .sorted(Comparator.comparingInt(MethodMetrics::getCyclomaticComplexity).reversed())
                .limit(10)
                .forEach(m -> sb.append("|").append(m.getClassName()).append("#").append(m.getMethodName()).append("|")
                        .append(m.getCyclomaticComplexity()).append("|")
                        .append(m.getLoc()).append("|\n"));
        sb.append("\n");

        sb.append("## 告警与改进建议\n\n");
        if (metrics.getAlerts().isEmpty()) {
            sb.append("无告警。\n\n");
        } else {
            sb.append("| 类型 | 目标 | 当前值 | 阈值 | 建议 |\n");
            sb.append("|---|---|---:|---:|---|\n");
            for (Alert alert : metrics.getAlerts()) {
                sb.append("|").append(alert.getType()).append("|")
                        .append(alert.getTarget()).append("|")
                        .append(alert.getValue()).append("|")
                        .append(alert.getThreshold()).append("|")
                        .append(alert.getSuggestion()).append("|\n");
            }
            sb.append("\n");
        }

        sb.append("## 度量结论\n\n");
        List<ClassMetrics> highRisk = metrics.getClasses().stream()
                .filter(c -> c.getWmc() > 50 || c.getCbo() > 14)
                .toList();
        if (highRisk.isEmpty()) {
            sb.append("当前项目整体可维护性较好，未发现明显高风险类。建议继续关注复杂度增长趋势。\n");
        } else {
            sb.append("发现 ").append(highRisk.size()).append(" 个高风险类，建议优先做职责拆分与依赖解耦。\n");
        }

        return sb.toString();
    }
}
