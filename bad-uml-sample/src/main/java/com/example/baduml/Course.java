package com.example.baduml;

import java.util.HashMap;
import java.util.Map;

public class Course {
    private String courseId;
    private String courseName;
    private final Map<String, Integer> scores = new HashMap<>();

    public void addCourse(String cid) {
        this.courseId = cid;
        this.courseName = "COURSE-" + cid;
        scores.put(cid + "-A", 60);
    }

    public void deleteCourse(String cid) {
        if (cid != null && cid.equals(courseId)) {
            scores.clear();
            courseName = null;
            courseId = null;
        }
    }

    public void modifyCourse(String cid) {
        if (cid == null) {
            return;
        }
        if (cid.contains("db")) {
            courseName = "Database-" + cid;
        } else if (cid.contains("se")) {
            courseName = "SoftwareEngineering-" + cid;
        } else {
            courseName = "General-" + cid;
        }
        scores.put(cid + "-B", 75);
        scores.put(cid + "-C", 90);
    }
}
