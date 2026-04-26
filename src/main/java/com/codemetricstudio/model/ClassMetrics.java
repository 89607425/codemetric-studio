package com.codemetricstudio.model;

public class ClassMetrics {
    private String className;
    private String packageName;

    // ===== CK 核心度量 =====
    private int wmc;   // Weighted Methods per Class (按教学口径：方法个数)
    private int dit;   // Depth of Inheritance Tree
    private int noc;   // Number of Children
    private int cbo;   // Coupling Between Objects
    private int rfc;   // Response for a Class
    private double lcom; // Lack of Cohesion of Methods

    // ===== 多态性度量 =====
    private int nop;    // Number of Polymorphic Methods (可被重写的方法数)
    private int nom;    // Number of Overridden Methods (实际重写的方法数)
    private int noo;    // Number of Overloads (重载方法数)
    private double pod; // Polymorphism Degree (多态度)
    private double overrideRatio; // 重写率 = 子类重写方法数 / 父类方法总数
    private double overloadRatio; // 重载率 = 重载方法数 / 所有方法数

    // ===== 扩展度量 =====
    private double sk;   // Specialization Index (特化指数)
    private int dac;    // Data Abstraction Coupling
    private int moa;    // Measure of Aggregation
    private double mfa;  // Measure of Functional Abstraction
    private double cam;  // Computational Abstraction Metric
    private int cis;    // Class Interface Size
    private int nsc;    // Number of Static Methods

    public ClassMetrics() {
    }

    // ===== CK 核心度量 getters/setters =====

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getWmc() {
        return wmc;
    }

    public void setWmc(int wmc) {
        this.wmc = wmc;
    }

    public int getDit() {
        return dit;
    }

    public void setDit(int dit) {
        this.dit = dit;
    }

    public int getNoc() {
        return noc;
    }

    public void setNoc(int noc) {
        this.noc = noc;
    }

    public int getCbo() {
        return cbo;
    }

    public void setCbo(int cbo) {
        this.cbo = cbo;
    }

    public int getRfc() {
        return rfc;
    }

    public void setRfc(int rfc) {
        this.rfc = rfc;
    }

    public double getLcom() {
        return lcom;
    }

    public void setLcom(double lcom) {
        this.lcom = lcom;
    }

    // ===== 多态性度量 getters/setters =====

    public int getNop() {
        return nop;
    }

    public void setNop(int nop) {
        this.nop = nop;
    }

    public int getNom() {
        return nom;
    }

    public void setNom(int nom) {
        this.nom = nom;
    }

    public int getNoo() {
        return noo;
    }

    public void setNoo(int noo) {
        this.noo = noo;
    }

    public double getPod() {
        return pod;
    }

    public void setPod(double pod) {
        this.pod = pod;
    }

    public double getOverrideRatio() {
        return overrideRatio;
    }

    public void setOverrideRatio(double overrideRatio) {
        this.overrideRatio = overrideRatio;
    }

    public double getOverloadRatio() {
        return overloadRatio;
    }

    public void setOverloadRatio(double overloadRatio) {
        this.overloadRatio = overloadRatio;
    }

    // ===== 扩展度量 getters/setters =====

    public double getSk() {
        return sk;
    }

    public void setSk(double sk) {
        this.sk = sk;
    }

    public int getDac() {
        return dac;
    }

    public void setDac(int dac) {
        this.dac = dac;
    }

    public int getMoa() {
        return moa;
    }

    public void setMoa(int moa) {
        this.moa = moa;
    }

    public double getMfa() {
        return mfa;
    }

    public void setMfa(double mfa) {
        this.mfa = mfa;
    }

    public double getCam() {
        return cam;
    }

    public void setCam(double cam) {
        this.cam = cam;
    }

    public int getCis() {
        return cis;
    }

    public void setCis(int cis) {
        this.cis = cis;
    }

    public int getNsc() {
        return nsc;
    }

    public void setNsc(int nsc) {
        this.nsc = nsc;
    }
}
