package com.codemetricstudio.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParsedMethod {
    private String classQualifiedName;
    private String methodName;
    private int complexity;
    private int loc;

    // 访问修饰符
    private boolean isStatic = false;
    private boolean isPrivate = false;
    private boolean isFinal = false;

    // 多态相关
    private boolean isOverridden = false;  // 是否重写了父类方法
    private String returnType = "";         // 返回类型
    private List<String> parameterTypes = new ArrayList<>();  // 参数类型列表
    private int overloadCount = 0;           // 与本类其他方法重载的数量

    // 调用和引用
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

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }

    public boolean isOverridden() {
        return isOverridden;
    }

    public void setOverridden(boolean overridden) {
        isOverridden = overridden;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(List<String> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public int getOverloadCount() {
        return overloadCount;
    }

    public void setOverloadCount(int overloadCount) {
        this.overloadCount = overloadCount;
    }
}
