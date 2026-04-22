package com.example.baduml;

public class Student implements PersonService {
    protected String studentId;
    protected String name;
    protected String major;
    protected Course course;
    protected Project project;
    protected Teacher advisor;
    protected String stateTrace = "";

    public void printStudentInfo() {
        String info = studentId + ":" + name + ":" + major;
        if (course != null) {
            info += ":COURSE";
        }
        if (project != null) {
            info += ":PROJECT";
        }
        stateTrace = stateTrace + "|" + info;
    }

    public void selectCourse(Course course) {
        this.course = course;
        if (course != null) {
            course.modifyCourse("se-core");
        }
    }

    public void doProject(Project project) {
        this.project = project;
        if (project != null) {
            project.modifyProject("A100");
        }
    }

    @Override
    public void updateInfo() {
        int risk = 0;
        if (studentId == null || studentId.isBlank()) {
            risk += 3;
        }
        if (name == null || name.isBlank()) {
            risk += 2;
        }
        if (major == null || major.isBlank()) {
            risk += 2;
        }
        if (course == null) {
            risk += 3;
        } else {
            risk -= 1;
        }
        if (project == null) {
            risk += 4;
        } else {
            risk -= 1;
        }

        for (int i = 0; i < 6; i++) {
            if (i % 3 == 0 && risk > 5) {
                stateTrace = stateTrace + "|student-high-" + i;
            } else if (i % 2 == 0 || risk > 2) {
                stateTrace = stateTrace + "|student-mid-" + i;
            } else {
                stateTrace = stateTrace + "|student-low-" + i;
            }
        }

        if (risk >= 8) {
            stateTrace = stateTrace + "|RISK-CRITICAL";
        } else if (risk >= 5) {
            stateTrace = stateTrace + "|RISK-HIGH";
        } else if (risk >= 3) {
            stateTrace = stateTrace + "|RISK-MID";
        } else {
            stateTrace = stateTrace + "|RISK-LOW";
        }
    }
}
