package com.codemetricstudio.web.model;

import com.codemetricstudio.config.ThresholdConfig;

import java.util.ArrayList;
import java.util.List;

public class WebAnalyzeRequest {
    private String projectName;
    private String module;
    private List<SourceFilePayload> sourceFiles = new ArrayList<>();
    private ThresholdConfig threshold;

    private String classDiagramText;
    private String flowDiagramText;
    private String cfgText;

    private FunctionPointInput functionPointInput;
    private UseCaseInput useCaseInput;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public List<SourceFilePayload> getSourceFiles() {
        return sourceFiles;
    }

    public void setSourceFiles(List<SourceFilePayload> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    public ThresholdConfig getThreshold() {
        return threshold;
    }

    public void setThreshold(ThresholdConfig threshold) {
        this.threshold = threshold;
    }

    public String getClassDiagramText() {
        return classDiagramText;
    }

    public void setClassDiagramText(String classDiagramText) {
        this.classDiagramText = classDiagramText;
    }

    public String getFlowDiagramText() {
        return flowDiagramText;
    }

    public void setFlowDiagramText(String flowDiagramText) {
        this.flowDiagramText = flowDiagramText;
    }

    public String getCfgText() {
        return cfgText;
    }

    public void setCfgText(String cfgText) {
        this.cfgText = cfgText;
    }

    public FunctionPointInput getFunctionPointInput() {
        return functionPointInput;
    }

    public void setFunctionPointInput(FunctionPointInput functionPointInput) {
        this.functionPointInput = functionPointInput;
    }

    public UseCaseInput getUseCaseInput() {
        return useCaseInput;
    }

    public void setUseCaseInput(UseCaseInput useCaseInput) {
        this.useCaseInput = useCaseInput;
    }
}
