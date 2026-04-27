package com.codemetricstudio.web.model;

public class DesignMetricsResult {
    private int ufp;
    private double functionPoint;
    private int uaw;
    private int uucw;
    private int uucp;
    private double useCasePoint;
    private int cfgCyclomaticComplexity;

    public int getUfp() {
        return ufp;
    }

    public void setUfp(int ufp) {
        this.ufp = ufp;
    }

    public double getFunctionPoint() {
        return functionPoint;
    }

    public void setFunctionPoint(double functionPoint) {
        this.functionPoint = functionPoint;
    }

    public int getUaw() {
        return uaw;
    }

    public void setUaw(int uaw) {
        this.uaw = uaw;
    }

    public int getUucw() {
        return uucw;
    }

    public void setUucw(int uucw) {
        this.uucw = uucw;
    }

    public int getUucp() {
        return uucp;
    }

    public void setUucp(int uucp) {
        this.uucp = uucp;
    }

    public double getUseCasePoint() {
        return useCasePoint;
    }

    public void setUseCasePoint(double useCasePoint) {
        this.useCasePoint = useCasePoint;
    }

    public int getCfgCyclomaticComplexity() {
        return cfgCyclomaticComplexity;
    }

    public void setCfgCyclomaticComplexity(int cfgCyclomaticComplexity) {
        this.cfgCyclomaticComplexity = cfgCyclomaticComplexity;
    }
}
