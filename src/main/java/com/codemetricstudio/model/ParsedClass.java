package com.codemetricstudio.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParsedClass {
    private String packageName;
    private String className;
    private String qualifiedName;
    private String superClass;
    private boolean isInterface;
    private Set<String> fieldNames = new HashSet<>();
    private Set<String> coupledTypes = new HashSet<>();
    private List<ParsedMethod> methods = new ArrayList<>();

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public String getSuperClass() {
        return superClass;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public void setInterface(boolean anInterface) {
        isInterface = anInterface;
    }

    public Set<String> getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(Set<String> fieldNames) {
        this.fieldNames = fieldNames;
    }

    public Set<String> getCoupledTypes() {
        return coupledTypes;
    }

    public void setCoupledTypes(Set<String> coupledTypes) {
        this.coupledTypes = coupledTypes;
    }

    public List<ParsedMethod> getMethods() {
        return methods;
    }

    public void setMethods(List<ParsedMethod> methods) {
        this.methods = methods;
    }
}
