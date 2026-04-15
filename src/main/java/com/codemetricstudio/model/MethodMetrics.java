package com.codemetricstudio.model;

public class MethodMetrics {
    private String className;
    private String methodName;
    private int cyclomaticComplexity;
    private int loc;

    public MethodMetrics() {
    }

    public MethodMetrics(String className, String methodName, int cyclomaticComplexity, int loc) {
        this.className = className;
        this.methodName = methodName;
        this.cyclomaticComplexity = cyclomaticComplexity;
        this.loc = loc;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public int getCyclomaticComplexity() {
        return cyclomaticComplexity;
    }

    public void setCyclomaticComplexity(int cyclomaticComplexity) {
        this.cyclomaticComplexity = cyclomaticComplexity;
    }

    public int getLoc() {
        return loc;
    }

    public void setLoc(int loc) {
        this.loc = loc;
    }
}
