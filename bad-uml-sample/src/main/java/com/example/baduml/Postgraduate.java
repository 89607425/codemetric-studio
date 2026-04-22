package com.example.baduml;

public class Postgraduate extends Student {
    @Override
    public void doProject(Project project) {
        super.doProject(project);
        if (project != null) {
            project.modifyProject("B200");
        }
    }
}
