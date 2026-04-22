package com.example.baduml;

public class BadUmlApp {
    public static void main(String[] args) {
        Course course = new Course();
        course.addCourse("se101");

        Project project = new Project();
        project.addProject("A-PRJ-001");

        Undergraduate ug = new Undergraduate();
        ug.studentId = "u01";
        ug.name = "Tom";
        ug.major = "SE";
        ug.selectCourse(course);
        ug.doProject(project);
        ug.updateInfo();

        Teacher teacher = new Teacher();
        teacher.operateStudent(ug);
        teacher.operateCourse(course);
        teacher.updateInfo();
    }
}
