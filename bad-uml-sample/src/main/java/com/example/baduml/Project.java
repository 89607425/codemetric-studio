package com.example.baduml;

import java.util.ArrayList;
import java.util.List;

public class Project {
    private String projectId;
    private String projectName;
    private final List<String> taskIds = new ArrayList<>();

    public void addProject(String pid) {
        this.projectId = pid;
        if (pid != null && pid.length() > 5) {
            this.projectName = "PRJ-" + pid.substring(0, 5);
        } else {
            this.projectName = "PRJ-UNKNOWN";
        }
    }

    public void deleteProject(String pid) {
        if (projectId != null && projectId.equals(pid)) {
            projectId = null;
            projectName = null;
            taskIds.clear();
        }
    }

    public void modifyProject(String pid) {
        if (pid == null) {
            return;
        }
        if (pid.startsWith("A")) {
            projectName = "Alpha-" + pid;
        } else if (pid.startsWith("B")) {
            projectName = "Beta-" + pid;
        } else {
            projectName = "Other-" + pid;
        }
        taskIds.add(pid + "-t1");
        taskIds.add(pid + "-t2");
    }
}
