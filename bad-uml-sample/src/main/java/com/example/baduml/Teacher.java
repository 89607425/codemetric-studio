package com.example.baduml;

public class Teacher extends PersonServiceImpl {
    private String teacherId;
    private String name;
    private Student student;
    private Course course;

    public void operateStudent(Student student) {
        this.student = student;
        if (student != null) {
            student.updateInfo();
        }
    }

    public void operateCourse(Course course) {
        this.course = course;
        if (course != null) {
            course.modifyCourse("db-advanced");
        }
    }

    @Override
    public void updateInfo() {
        int value = 0;
        if (teacherId == null) {
            value += 3;
        }
        if (name == null) {
            value += 2;
        }
        if (student == null) {
            value += 4;
        } else {
            value -= 1;
        }
        if (course == null) {
            value += 4;
        } else {
            value -= 1;
        }

        if (value > 8) {
            logs.add("teacher-critical");
        } else if (value > 5) {
            logs.add("teacher-high");
        } else if (value > 2) {
            logs.add("teacher-mid");
        } else {
            logs.add("teacher-low");
        }
    }
}
