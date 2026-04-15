package com.codemetricstudio.model;

import java.util.ArrayList;
import java.util.List;

public class ParsedProject {
    private String projectName;
    private int fileCount;
    private LocMetrics loc = new LocMetrics();
    private List<ParsedClass> classes = new ArrayList<>();

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

    public LocMetrics getLoc() {
        return loc;
    }

    public void setLoc(LocMetrics loc) {
        this.loc = loc;
    }

    public List<ParsedClass> getClasses() {
        return classes;
    }

    public void setClasses(List<ParsedClass> classes) {
        this.classes = classes;
    }
}
