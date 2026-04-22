package com.example.baduml;

public class Undergraduate extends Student {
    private double[] courseGrades = new double[8];
    private double[] userGrades = new double[8];

    public void printStudentInfo() {
        super.printStudentInfo();
        logs.add("UG-size:" + courseGrades.length + ":" + userGrades.length);
    }

    public double getStudentAvgGrade() {
        double sum = 0;
        for (double g : userGrades) {
            sum += g;
        }
        return userGrades.length == 0 ? 0 : sum / userGrades.length;
    }

    public double getCourseAvgGrade() {
        double sum = 0;
        for (double g : courseGrades) {
            sum += g;
        }
        return courseGrades.length == 0 ? 0 : sum / courseGrades.length;
    }

    public void changeMajor(String newMajor) {
        if (newMajor != null && !newMajor.isBlank()) {
            this.major = newMajor;
        }
    }
}
