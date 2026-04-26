package com.codemetricstudio.model;

/**
 * 多态性度量指标
 *
 * 度量理论：
 * - 多态性是面向对象的核心特性之一
 * - 适度多态：表明良好的继承结构设计
 * - 过多/过少多态：可能表明设计问题
 */
public class PolymorphismMetrics {
    /**
     * NOP (Number of Polymorphic Methods) - 多态方法数
     * 类中可被重写的方法数量
     * 反映类的多态潜力
     */
    private int nop;

    /**
     * NOM (Number of Overridden Methods) - 重写方法数
     * 实际被重写的方法数量
     * 反映多态的实际使用程度
     */
    private int nom;

    /**
     * NOO (Number of Overloads) - 重载方法数
     * 同一类中方法名相同但参数不同的方法数量
     * 反映方法的灵活性
     */
    private int noo;

    /**
     * NOA (Number of Annotations) - 注解数量
     * 类或方法上的注解数量
     * 反映框架依赖程度
     */
    private int noa;

    /**
     * POD (Polymorphism Degree) - 多态度
     * NOM / NOP，反映多态被实际使用的比例
     * 0 = 未使用多态，1 = 完全多态
     */
    private double pod;

    /**
     * 继承层次中的多态覆盖因子
     * 从父类继承的方法中被重写的比例
     */
    private double inheritedPolymorphismFactor;

    /**
     * Override Ratio (重写率)
     * = 子类重写方法数 / 父类方法总数
     * 反映子类对父类方法的覆盖程度
     */
    private double overrideRatio;

    /**
     * Overload Ratio (重载率)
     * = 重载方法数 / 所有方法数
     * 反映方法重载的使用程度
     */
    private double overloadRatio;

    public PolymorphismMetrics() {
    }

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

    public int getNoa() {
        return noa;
    }

    public void setNoa(int noa) {
        this.noa = noa;
    }

    public double getPod() {
        return pod;
    }

    public void setPod(double pod) {
        this.pod = pod;
    }

    public double getInheritedPolymorphismFactor() {
        return inheritedPolymorphismFactor;
    }

    public void setInheritedPolymorphismFactor(double inheritedPolymorphismFactor) {
        this.inheritedPolymorphismFactor = inheritedPolymorphismFactor;
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
}
