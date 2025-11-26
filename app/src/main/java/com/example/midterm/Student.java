package com.example.midterm;

public class Student {
    private String id; // Firestore Document ID
    private String name;
    private String studentId; // The manual ID (e.g., S123)
    private String sClass;

    public Student() { } // Empty constructor needed for Firebase

    public Student(String id, String name, String studentId, String sClass) {
        this.id = id;
        this.name = name;
        this.studentId = studentId;
        this.sClass = sClass;
    }

    public String getName() { return name; }
    public String getStudentId() { return studentId; }
    public String getSClass() { return sClass; }
}
