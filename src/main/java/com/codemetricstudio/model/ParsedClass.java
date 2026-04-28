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

    // 字段信息
    private Set<String> fieldNames = new HashSet<>();
    private Set<String> fieldTypes = new HashSet<>();  // 字段的类型（用于MOA计算）

    // 耦合信息
    private Set<String> coupledTypes = new HashSet<>();

    // 方法列表
    private List<ParsedMethod> methods = new ArrayList<>();

    // 实现的接口列表
    private Set<String> implementedInterfaces = new HashSet<>();

    // 注解数量（用于多态度量）
    private int annotationCount = 0;

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

    public Set<String> getFieldTypes() {
        return fieldTypes;
    }

    public void setFieldTypes(Set<String> fieldTypes) {
        this.fieldTypes = fieldTypes;
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

    public int getAnnotationCount() {
        return annotationCount;
    }

    public void setAnnotationCount(int annotationCount) {
        this.annotationCount = annotationCount;
    }

    public Set<String> getImplementedInterfaces() {
        return implementedInterfaces;
    }

    public void setImplementedInterfaces(Set<String> implementedInterfaces) {
        this.implementedInterfaces = implementedInterfaces;
    }
}
