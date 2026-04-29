package com.codemetricstudio.metrics;

import com.codemetricstudio.model.ExtendedMetrics;
import com.codemetricstudio.model.ParsedClass;
import com.codemetricstudio.model.ParsedMethod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 扩展面向对象度量计算器
 *
 * 这些指标与CK指标互补，从不同角度评估类的质量。
 * 特别适合评估类的设计复杂度和可维护性。
 *
 * 核心指标及特点：
 *
 * 1. SK (Specialization Index) - 特化指数
 *    特点：衡量类在继承层次中的特化程度
 *    公式：SK = NOC / DIT 或 1 + 1/DIT
 *    解读：高的SK值表明继承层次设计良好，父子类职责分配合理
 *
 * 2. DAC (Data Abstraction Coupling) - 数据抽象耦合
 *    特点：衡量类的"抽象宽度"，反映其与其他类的关联程度
 *    计算：类中作为参数、局部变量、返回类型的用户定义类型数
 *    解读：高DAC表明类具有较高的抽象能力，但过高可能意味着过度耦合
 *
 * 3. MOA (Measure of Aggregation) - 聚合度量
 *    特点：衡量类的组合程度，反映其作为整体的能力
 *    计算：作为实例变量的用户定义类型数
 *    解读：高MOA表明良好的组合设计，但过高可能意味着类承担过多聚合职责
 *
 * 4. CAM (Computational Abstraction Metric) - 计算抽象度量
 *    特点：衡量参数类型设计的复用程度
 *    计算：CAM = (r/t) * (1/t)，t=参数类型数，r=使用多种类型参数的方法数
 *    解读：高CAM表明参数设计具有良好的类型复用性
 *
 * 6. CIS (Class Interface Size) - 类接口大小
 *    特点：衡量类的公共接口复杂度
 *    计算：public方法数量
 *    解读：高CIS可能表明类承担过多职责（违反单一职责原则）
 */
public class ExtendedMetricsCalculator {

    /**
     * 计算类的扩展度量
     *
     * @param parsedClass 解析后的类信息
     * @param dit 继承深度
     * @param noc 子类数量
     * @param parentClass 父类信息（用于继承相关指标计算）
     * @param projectClasses 项目中所有类（用于计算DAC等跨类指标）
     * @return 扩展度量结果
     */
    public ExtendedMetrics calculate(ParsedClass parsedClass, int dit, int noc, ParsedClass parentClass, List<ParsedClass> projectClasses) {
        ExtendedMetrics metrics = new ExtendedMetrics();

        // SK: 特化指数 = NOC / DIT
        metrics.setSk(calculateSpecializationIndex(noc, dit));

        // DAC: 数据抽象耦合
        metrics.setDac(calculateDac(parsedClass, projectClasses));

        // MOA: 聚合度量
        metrics.setMoa(calculateMoa(parsedClass, projectClasses));

        // CAM: 计算抽象度量
        metrics.setCam(calculateCam(parsedClass));

        // CIS: 类接口大小 = public方法数
        metrics.setCis(calculateCis(parsedClass));

        // ADC: 调用图平均继承深度（简化计算）
        metrics.setAdc(calculateAdc(parsedClass));

        metrics.setDit(dit);
        metrics.setNoc(noc);

        // AIF: 属性继承因子
        metrics.setAif(calculateAif(parsedClass, parentClass));

        // MIF: 方法继承因子
        metrics.setMif(calculateMif(parsedClass, parentClass));

        return metrics;
    }

    /**
     * 计算属性继承因子 (Attribute Inheritance Factor)
     * AIF = 继承的属性数 / 总属性数
     * 继承的属性 = 直接父类中定义但子类中未定义的属性
     */
    private double calculateAif(ParsedClass parsedClass, ParsedClass parentClass) {
        int totalFields = parsedClass.getFieldNames().size();
        // 如果类没有定义任何属性，继承因子为0（无自身属性则无法计算继承比例）
        if (totalFields == 0) {
            return 0.0;
        }

        if (parentClass == null) {
            return 0.0; // 无父类，无继承属性
        }

        // 只计算直接父类的继承属性
        int inheritedFields = 0;
        for (String field : parentClass.getFieldNames()) {
            if (!parsedClass.getFieldNames().contains(field)) {
                inheritedFields++;
            }
        }

        return (double) inheritedFields / totalFields;
    }

    /**
     * 计算方法继承因子 (Method Inheritance Factor)
     * MIF = 继承的方法数 / 总方法数
     * 继承的方法 = 直接父类中定义的方法中，子类未重写且未新增同名的方法
     */
    private double calculateMif(ParsedClass parsedClass, ParsedClass parentClass) {
        if (parentClass == null) {
            return 0.0; // 无父类，无继承方法
        }

        // 只计算直接父类的继承方法
        int inheritedMethods = 0;

        // 收集本类所有方法签名（包括继承的）
        Set<String> childSignatures = new HashSet<>();
        for (ParsedMethod method : parsedClass.getMethods()) {
            childSignatures.add(getMethodSignature(method));
        }

        // 遍历直接父类的方法
        for (ParsedMethod parentMethod : parentClass.getMethods()) {
            // 跳过私有和静态方法
            if (parentMethod.isPrivate() || parentMethod.isStatic()) {
                continue;
            }

            String sig = getMethodSignature(parentMethod);

            // 如果子类已经重写了这个方法（签名相同），则不算继承
            if (!childSignatures.contains(sig)) {
                inheritedMethods++;
            }
        }

        int totalMethods = parsedClass.getMethods().size() + inheritedMethods;
        if (totalMethods == 0) {
            return 0.0;
        }

        return (double) inheritedMethods / totalMethods;
    }

    /**
     * 获取方法的签名（方法名 + 参数类型）
     */
    private String getMethodSignature(ParsedMethod method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getMethodName());
        sb.append("(");
        for (int i = 0; i < method.getParameterTypes().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(method.getParameterTypes().get(i));
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * 计算特化指数 (Specialization Index)
     * SK = NOC / DIT
     * DIT=0时返回0，NOC=0时返回小值
     */
    private double calculateSpecializationIndex(int noc, int dit) {
        if (dit == 0) {
            return 0.0;
        }
        return (double) noc / dit;
    }

    /**
     * 计算数据抽象耦合 (Data Abstraction Coupling)
     * 类中作为参数、局部变量、返回类型、字段类型使用的用户定义类型数量
     */
    private int calculateDac(ParsedClass parsedClass, List<ParsedClass> projectClasses) {
        Set<String> userDefinedTypes = new HashSet<>();

        // 获取字段类型中的用户定义类型
        for (String fieldType : parsedClass.getFieldTypes()) {
            if (isUserDefinedType(fieldType, projectClasses)) {
                userDefinedTypes.add(fieldType);
            }
        }

        for (ParsedMethod method : parsedClass.getMethods()) {
            // 获取方法参数中的用户定义类型
            for (String paramType : method.getParameterTypes()) {
                if (isUserDefinedType(paramType, projectClasses)) {
                    userDefinedTypes.add(paramType);
                }
            }
            // 获取返回类型中的用户定义类型
            if (isUserDefinedType(method.getReturnType(), projectClasses)) {
                userDefinedTypes.add(method.getReturnType());
            }
        }

        return userDefinedTypes.size();
    }

    /**
     * 判断是否为用户定义类型（非Java内置类型）
     */
    private boolean isUserDefinedType(String type, List<ParsedClass> projectClasses) {
        if (type == null || type.isEmpty()) {
            return false;
        }

        // 跳过Java内置类型
        if (isBuiltinType(type)) {
            return false;
        }

        // 检查是否为项目中的类
        for (ParsedClass pc : projectClasses) {
            if (pc.getQualifiedName().equals(type) || pc.getClassName().equals(type)) {
                return true;
            }
        }

        // 如果不是Java内置类型，也不是已知项目类，也计入（可能是外部库类型）
        return !type.startsWith("java.") && !type.startsWith("javax.");
    }

    /**
     * 判断是否为Java内置类型
     */
    private boolean isBuiltinType(String type) {
        if (type == null) return true;

        String baseType = type.replace("[]", "").replace("<", " ").replace(">", "").split(" ")[0].split("<")[0];

        return baseType.equals("void") || baseType.equals("boolean") ||
               baseType.equals("byte") || baseType.equals("char") ||
               baseType.equals("short") || baseType.equals("int") ||
               baseType.equals("long") || baseType.equals("float") ||
               baseType.equals("double") || baseType.equals("String") ||
               baseType.equals("Object") || baseType.equals("Class");
    }

    /**
     * 计算聚合度量 (Measure of Aggregation)
     * 作为实例变量的用户定义类型数量
     */
    private int calculateMoa(ParsedClass parsedClass, List<ParsedClass> projectClasses) {
        int count = 0;
        for (String fieldType : parsedClass.getFieldTypes()) {
            if (isUserDefinedType(fieldType, projectClasses)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 计算计算抽象度量 (Computational Abstraction Metric)
     * CAM = (r/t) * (1/t) 其中 t=参数类型数，r=使用多种类型参数的方法数
     */
    private double calculateCam(ParsedClass parsedClass) {
        int totalMethods = parsedClass.getMethods().size();
        if (totalMethods == 0) {
            return 0.0;
        }

        double sum = 0.0;
        for (ParsedMethod method : parsedClass.getMethods()) {
            List<String> paramTypes = method.getParameterTypes();
            int typeCount = getDistinctTypeCount(paramTypes);
            int paramCount = paramTypes.size();

            if (paramCount > 0 && typeCount > 0) {
                sum += ((double) typeCount / paramCount) * (1.0 / typeCount);
            }
        }

        return sum / totalMethods;
    }

    /**
     * 获取参数类型中不同类型的数量
     */
    private int getDistinctTypeCount(List<String> types) {
        Set<String> distinct = new HashSet<>();
        for (String type : types) {
            distinct.add(extractBaseType(type));
        }
        return distinct.size();
    }

    /**
     * 提取基础类型名
     */
    private String extractBaseType(String type) {
        if (type == null) return "";
        String baseType = type.replace("[]", "").split("<")[0];
        // 处理泛型
        if (type.contains("<")) {
            int start = type.indexOf('<');
            int end = type.lastIndexOf('>');
            if (start > 0 && end > start) {
                baseType = type.substring(0, start);
            }
        }
        return baseType.trim();
    }

    /**
     * 计算类接口大小 (Class Interface Size)
     * public方法数量
     */
    private int calculateCis(ParsedClass parsedClass) {
        int count = 0;
        for (ParsedMethod method : parsedClass.getMethods()) {
            if (!method.isPrivate() && !method.isStatic()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 计算调用图平均继承深度（简化版本）
     */
    private double calculateAdc(ParsedClass parsedClass) {
        if (parsedClass.getMethods().isEmpty()) {
            return 0.0;
        }

        // 简化：使用继承深度作为代理
        // 实际应计算方法调用的继承深度
        return 1.0; // 简化处理
    }
}
