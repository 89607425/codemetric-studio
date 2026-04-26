package com.codemetricstudio.model;

/**
 * 其他重要面向对象度量指标
 *
 * 这些指标与CK指标互补，从不同角度评估类的质量
 */
public class ExtendedMetrics {
    /**
     * SK (Specialization Index) - 特化指数
     * 度量类在继承层次中的特化程度
     * SK = 1 + 1/DIT 或 SK = NOC/DIT
     * 特点：反映继承层次的设计深度
     */
    private double sk;

    /**
     * DAC (Data Abstraction Coupling) - 数据抽象耦合
     * 类中作为参数、局部变量或返回类型使用的用户定义类型数
     * 特点：衡量类的"宽度"，反映其抽象能力
     */
    private int dac;

    /**
     * MOA (Measure of Aggregation) - 聚合度量
     * 类中作为实例变量的用户定义类型数量
     * 特点：反映类的组合/聚合程度
     * 高MOA表明强聚合，但过高可能意味着职责过多
     */
    private int moa;

    /**
     * MFA (Measure of Functional Abstraction) - 功能抽象度量
     * 类中调用的外部方法数占所有方法调用的比例
     * 特点：衡量类的抽象程度和依赖反转倾向
     * 高MFA表明良好的抽象，但过高可能导致过度设计
     */
    private double mfa;

    /**
     * CAM (Computational Abstraction Metric) - 计算抽象度量
     * 一个方法中多个不同类型参数被使用的次数
     * CAM = (r/t) * (1/t) 其中 t=参数类型数，r=使用多种类型的参数的方法数
     * 特点：衡量参数类型的复用程度，高值表明参数设计良好
     */
    private double cam;

    /**
     * CIS (Class Interface Size) - 类接口大小
     * 类的public方法数量
     * 特点：反映类的复杂度和职责范围
     * 高CIS可能表明类承担过多职责
     */
    private int cis;

    /**
     * NSC (Number of Static Methods) - 静态方法数
     * 类的静态方法数量
     * 特点：高静态方法数可能表明过程式设计倾向
     */
    private int nsc;

    /**
     * ADC (Average Depth of Inheritance in Call graph) - 调用图平均继承深度
     * 方法调用链的平均继承深度
     * 特点：反映代码的深层复杂性
     */
    private double adc;

    /**
     * DIT (Depth of Inheritance Tree) - 继承树深度
     * 已有，从CK
     */
    private int dit;

    /**
     * NOC (Number of Children) - 子类数量
     * 已有，从CK
     */
    private int noc;

    public ExtendedMetrics() {
    }

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

    public double getAdc() {
        return adc;
    }

    public void setAdc(double adc) {
        this.adc = adc;
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
}
