package com.codemetricstudio.model;

import java.util.ArrayList;
import java.util.List;

public class ProjectMetrics {
    private String projectName;
    private int fileCount;
    private int classCount;
    private int methodCount;
    private LocMetrics loc = new LocMetrics();
    private List<ClassMetrics> classes = new ArrayList<>();
    private List<MethodMetrics> methods = new ArrayList<>();
    private List<Alert> alerts = new ArrayList<>();

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public int getClassCount() {
        return classCount;
    }

    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }

    public int getMethodCount() {
        return methodCount;
    }

    public void setMethodCount(int methodCount) {
        this.methodCount = methodCount;
    }

    public LocMetrics getLoc() {
        return loc;
    }

    public void setLoc(LocMetrics loc) {
        this.loc = loc;
    }

    public List<ClassMetrics> getClasses() {
        return classes;
    }

    public void setClasses(List<ClassMetrics> classes) {
        this.classes = classes;
    }

    public List<MethodMetrics> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodMetrics> methods) {
        this.methods = methods;
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }
}
