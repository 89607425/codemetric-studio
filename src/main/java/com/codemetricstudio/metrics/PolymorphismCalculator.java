package com.codemetricstudio.metrics;

import com.codemetricstudio.model.ParsedClass;
import com.codemetricstudio.model.ParsedMethod;
import com.codemetricstudio.model.PolymorphismMetrics;

import java.util.HashSet;
import java.util.Set;

/**
 * 多态性度量计算器
 *
 * 多态性是面向对象编程的核心特性，合理的度量有助于评估继承结构的质量。
 *
 * 核心指标：
 * - NOP (Number of Polymorphic Methods): 可被重写的方法数
 * - NOM (Number of Overridden Methods): 实际被重写的方法数
 * - NOO (Number of Overloads): 重载方法数
 * - POD (Polymorphism Degree): 多态度 = NOM/NOP
 *
 * 解读：
 * - POD 接近 0: 类有多态潜力但未使用，可能需要重新设计
 * - POD 接近 1: 良好使用了多态
 * - POD 过低但有很多继承: 继承层次可能过于扁平
 */
public class PolymorphismCalculator {

    /**
     * 计算类的多态性度量
     *
     * @param parsedClass 解析后的类信息
     * @param parentClass 父类信息（如果有）
     * @return 多态性度量结果
     */
    public PolymorphismMetrics calculate(ParsedClass parsedClass, ParsedClass parentClass) {
        PolymorphismMetrics metrics = new PolymorphismMetrics();

        // NOP: 可被重写的方法数量（非static、非private、非final的实例方法）
        int nop = countPolymorphicMethods(parsedClass);
        metrics.setNop(nop);

        // NOM: 实际被重写的方法数量
        int nom = countOverriddenMethods(parsedClass, parentClass);
        metrics.setNom(nom);

        // NOO: 重载方法数量
        int noo = countOverloadedMethods(parsedClass);
        metrics.setNoo(noo);

        // NOA: 注解数量
        metrics.setNoa(parsedClass.getAnnotationCount());

        // POD: 多态度
        double pod = calculatePolymorphismDegree(nop, nom);
        metrics.setPod(pod);

        // 继承多态因子
        double ipf = calculateInheritedPolymorphismFactor(parsedClass, parentClass);
        metrics.setInheritedPolymorphismFactor(ipf);

        // Override Ratio (重写率) = 子类重写方法数 / 父类方法总数
        double overrideRatio = calculateOverrideRatio(parsedClass, parentClass);
        metrics.setOverrideRatio(overrideRatio);

        // Overload Ratio (重载率) = 重载方法数 / 所有方法数
        double overloadRatio = calculateOverloadRatio(parsedClass);
        metrics.setOverloadRatio(overloadRatio);

        return metrics;
    }

    /**
     * 计算可被重写的方法数量
     * 可被重写的方法：非static、非private、非final的实例方法
     */
    private int countPolymorphicMethods(ParsedClass parsedClass) {
        int count = 0;
        for (ParsedMethod method : parsedClass.getMethods()) {
            if (isPolymorphic(method)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 判断方法是否可被重写
     */
    private boolean isPolymorphic(ParsedMethod method) {
        return !method.isStatic()
                && !method.isPrivate()
                && !method.isFinal();
    }

    /**
     * 计算实际被重写的方法数量
     * 方法签名与父类相同时视为重写
     * 不再依赖parentClass参数，直接使用isOverridden标志
     */
    private int countOverriddenMethods(ParsedClass parsedClass, ParsedClass parentClass) {
        int count = 0;
        for (ParsedMethod method : parsedClass.getMethods()) {
            if (method.isOverridden()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 计算重载方法数量（NOO）
     * 教学定义：每组重载的重载数之和 = 每组方法数 - 1 的总和
     * 计算公式：NOO = 总方法数 - 不同方法名的数量
     * 例如：类有方法 [add(int), add(double), subtract(int)] = 3个方法，2个不同方法名
     *       NOO = (2-1) + (1-1) = 1
     */
    private int countOverloadedMethods(ParsedClass parsedClass) {
        int totalMethods = parsedClass.getMethods().size();
        if (totalMethods <= 1) {
            return 0;
        }

        // 统计不同方法名的数量
        Set<String> uniqueMethodNames = new HashSet<>();
        for (ParsedMethod method : parsedClass.getMethods()) {
            uniqueMethodNames.add(method.getMethodName());
        }

        // NOO = 总方法数 - 不同方法名的数量
        int noo = totalMethods - uniqueMethodNames.size();
        return Math.max(0, noo);
    }

    /**
     * 计算多态度 (Polymorphism Degree)
     * POD = NOM / NOP
     * 如果 NOP 为 0，返回 0
     */
    private double calculatePolymorphismDegree(int nop, int nom) {
        if (nop == 0) {
            return 0.0;
        }
        return (double) nom / nop;
    }

    /**
     * 计算继承多态因子
     * 父类中被重写的方法占总可重写方法的比例
     */
    private double calculateInheritedPolymorphismFactor(ParsedClass parsedClass, ParsedClass parentClass) {
        if (parentClass == null) {
            return 0.0;
        }

        int parentPolymorphic = countPolymorphicMethods(parentClass);
        if (parentPolymorphic == 0) {
            return 0.0;
        }

        int overriddenCount = countOverriddenMethods(parsedClass, parentClass);
        return (double) overriddenCount / parentPolymorphic;
    }

    /**
     * 计算重写率 (Override Ratio)
     * Override Ratio = 子类重写方法数 / 父类方法总数
     * 如果没有父类，返回 0
     */
    private double calculateOverrideRatio(ParsedClass parsedClass, ParsedClass parentClass) {
        if (parentClass == null) {
            return 0.0;
        }

        int parentMethodCount = parentClass.getMethods().size();
        if (parentMethodCount == 0) {
            return 0.0;
        }

        int overriddenCount = countOverriddenMethods(parsedClass, parentClass);
        return (double) overriddenCount / parentMethodCount;
    }

    /**
     * 计算重载率 (Overload Ratio)
     * Overload Ratio = 重载方法数 / 所有方法数
     * 如果类没有方法，返回 0
     */
    private double calculateOverloadRatio(ParsedClass parsedClass) {
        int totalMethods = parsedClass.getMethods().size();
        if (totalMethods == 0) {
            return 0.0;
        }

        int overloadedMethods = countOverloadedMethods(parsedClass);
        return (double) overloadedMethods / totalMethods;
    }
}
