package com.codemetricstudio.model;

import java.util.HashSet;
import java.util.Set;

public class ParsedMethod {
    private String classQualifiedName;
    private String methodName;
    private int complexity;
    private int loc;
    private Set<String> referencedFields = new HashSet<>();
    private Set<String> calledMethods = new HashSet<>();

    public String getClassQualifiedName() {
        return classQualifiedName;
    }

    public void setClassQualifiedName(String classQualifiedName) {
        this.classQualifiedName = classQualifiedName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public int getComplexity() {
        return complexity;
    }

    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }

    public int getLoc() {
        return loc;
    }

    public void setLoc(int loc) {
        this.loc = loc;
    }

    public Set<String> getReferencedFields() {
        return referencedFields;
    }

    public void setReferencedFields(Set<String> referencedFields) {
        this.referencedFields = referencedFields;
    }

    public Set<String> getCalledMethods() {
        return calledMethods;
    }

    public void setCalledMethods(Set<String> calledMethods) {
        this.calledMethods = calledMethods;
    }
}
