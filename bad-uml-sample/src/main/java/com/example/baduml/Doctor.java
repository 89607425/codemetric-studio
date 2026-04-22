package com.example.baduml;

public class Doctor extends Postgraduate {
    public void courseAssessment() {
        if (course != null) {
            course.modifyCourse("doctor-research");
        }
        if (project != null) {
            project.modifyProject("D300");
        }
        updateInfo();
    }
}
