package com.codemetricstudio.web;

import com.codemetricstudio.aggregator.MetricsAggregator;
import com.codemetricstudio.config.ThresholdConfig;
import com.codemetricstudio.metrics.InMemoryProjectAnalyzer;
import com.codemetricstudio.model.ProjectMetrics;
import com.codemetricstudio.reporter.MarkdownReporter;
import com.codemetricstudio.web.model.DesignMetricsResult;
import com.codemetricstudio.web.model.FunctionPointInput;
import com.codemetricstudio.web.model.UseCaseInput;
import com.codemetricstudio.web.model.WebAnalyzeRequest;
import com.codemetricstudio.web.model.WebAnalyzeResponse;

public class WebAnalysisService {

    private final InMemoryProjectAnalyzer analyzer = new InMemoryProjectAnalyzer();
    private final MetricsAggregator aggregator = new MetricsAggregator();
    private final MarkdownReporter markdownReporter = new MarkdownReporter();

    public WebAnalyzeResponse analyze(WebAnalyzeRequest request) {
        long start = System.currentTimeMillis();
        ThresholdConfig threshold = request.getThreshold() == null ? new ThresholdConfig() : request.getThreshold();

        ProjectMetrics projectMetrics = aggregator.aggregate(
                analyzer.analyze(request.getProjectName(), request.getSourceFiles(), request.getModule()),
                threshold
        );

        DesignMetricsResult designMetrics = calculateDesignMetrics(request);

        WebAnalyzeResponse response = new WebAnalyzeResponse();
        response.setProjectMetrics(projectMetrics);
        response.setDesignMetrics(designMetrics);
        response.setMarkdownReport(markdownReporter.build(projectMetrics));
        response.setElapsedMs(System.currentTimeMillis() - start);
        return response;
    }

    private DesignMetricsResult calculateDesignMetrics(WebAnalyzeRequest request) {
        DesignMetricsResult result = new DesignMetricsResult();

        FunctionPointInput fp = request.getFunctionPointInput();
        if (fp != null) {
            int ufp = fp.getEi() * 4 + fp.getEo() * 5 + fp.getEq() * 4 + fp.getIlf() * 10 + fp.getEif() * 7;
            result.setUfp(ufp);
            result.setFunctionPoint(round2(ufp * fp.getVaf()));
        }

        UseCaseInput uc = request.getUseCaseInput();
        if (uc != null) {
            int uaw = uc.getActorSimple() + uc.getActorAverage() * 2 + uc.getActorComplex() * 3;
            int uucw = uc.getUcSimple() * 5 + uc.getUcAverage() * 10 + uc.getUcComplex() * 15;
            int uucp = uaw + uucw;
            result.setUaw(uaw);
            result.setUucw(uucw);
            result.setUucp(uucp);
            result.setUseCasePoint(round2(uucp * uc.getTcf() * uc.getEcf()));
        }

        String cfgText = request.getCfgText() == null ? "" : request.getCfgText();
        result.setCfgCyclomaticComplexity(estimateComplexity(cfgText));

        return result;
    }

    private int estimateComplexity(String code) {
        int ifCount = count(code, "\\bif\\b");
        int forCount = count(code, "\\bfor\\b");
        int whileCount = count(code, "\\bwhile\\b");
        int caseCount = count(code, "\\bcase\\b");
        int catchCount = count(code, "\\bcatch\\b");
        int andCount = count(code, "&&");
        int orCount = count(code, "\\|\\|");
        int ternaryCount = count(code, "\\?");
        return 1 + ifCount + forCount + whileCount + caseCount + catchCount + andCount + orCount + ternaryCount;
    }

    private int count(String source, String regex) {
        return (int) java.util.regex.Pattern.compile(regex).matcher(source).results().count();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
