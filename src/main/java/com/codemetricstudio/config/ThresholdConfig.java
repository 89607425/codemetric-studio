package com.codemetricstudio.config;

public class ThresholdConfig {
    private int methodComplexity = 10;
    private int classWmc = 50;
    private int classCbo = 14;

    public int getMethodComplexity() {
        return methodComplexity;
    }

    public void setMethodComplexity(int methodComplexity) {
        this.methodComplexity = methodComplexity;
    }

    public int getClassWmc() {
        return classWmc;
    }

    public void setClassWmc(int classWmc) {
        this.classWmc = classWmc;
    }

    public int getClassCbo() {
        return classCbo;
    }

    public void setClassCbo(int classCbo) {
        this.classCbo = classCbo;
    }
}
