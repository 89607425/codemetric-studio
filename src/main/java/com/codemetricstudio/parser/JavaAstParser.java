package com.codemetricstudio.parser;

import com.codemetricstudio.metrics.CyclomaticComplexityCalculator;
import com.codemetricstudio.model.ParsedClass;
import com.codemetricstudio.model.ParsedMethod;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JavaAstParser {

    private final CyclomaticComplexityCalculator complexityCalculator = new CyclomaticComplexityCalculator();

    public JavaAstParser() {
        ParserConfiguration configuration = new ParserConfiguration();
        StaticJavaParser.setConfiguration(configuration);
    }

    /**
     * 解析文件，返回类列表
     */
    public List<ParsedClass> parseFile(Path file) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(Files.readString(file));
            String pkg = cu.getPackageDeclaration().map(pd -> pd.getName().asString()).orElse("");

            List<ParsedClass> classes = new ArrayList<>();
            for (ClassOrInterfaceDeclaration declaration : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                classes.add(parseClass(pkg, declaration));
            }
            return classes;
        } catch (IOException | RuntimeException ex) {
            return List.of();
        }
    }

    /**
     * 两阶段处理：
     * 1. 解析所有类
     * 2. 检测重写方法
     * 需要在所有类都解析完成后调用此方法
     */
    public void detectOverriddenMethods(List<ParsedClass> allClasses) {
        // 构建类名到类的映射
        Map<String, ParsedClass> classMap = new HashMap<>();
        for (ParsedClass clazz : allClasses) {
            classMap.put(clazz.getClassName(), clazz);
            classMap.put(clazz.getQualifiedName(), clazz);
        }

        // 构建每个类的父类+父接口的方法签名集合
        Map<String, Set<String>> parentMethodSignatures = new HashMap<>();
        for (ParsedClass clazz : allClasses) {
            Set<String> signatures = new HashSet<>();
            Set<String> visited = new HashSet<>();

            // 收集直接父类的方法（递归向上）
            collectParentMethods(clazz, classMap, signatures, visited);

            // 收集所有实现的接口的方法（包括继承链上的类实现的接口）
            collectAllInterfaceMethods(clazz, classMap, signatures);

            if (!signatures.isEmpty()) {
                parentMethodSignatures.put(clazz.getQualifiedName(), signatures);
            }
        }

        // 遍历所有类，检测重写方法
        for (ParsedClass clazz : allClasses) {
            Set<String> parentSigs = parentMethodSignatures.get(clazz.getQualifiedName());
            if (parentSigs != null) {
                for (ParsedMethod method : clazz.getMethods()) {
                    String sig = getMethodSignature(method);
                    if (parentSigs.contains(sig) && !method.isStatic() && !method.isPrivate()) {
                        method.setOverridden(true);
                    }
                }
            }
        }
    }

    /**
     * 递归收集父类的方法
     */
    private void collectParentMethods(ParsedClass clazz, Map<String, ParsedClass> classMap,
                                      Set<String> signatures, Set<String> visited) {
        if (visited.contains(clazz.getQualifiedName())) {
            return; // 防止循环继承
        }
        visited.add(clazz.getQualifiedName());

        String superClass = clazz.getSuperClass();
        if (superClass != null && !superClass.isBlank()) {
            ParsedClass parent = classMap.get(superClass);
            if (parent != null) {
                for (ParsedMethod method : parent.getMethods()) {
                    signatures.add(getMethodSignature(method));
                }
                // 继续向上收集
                collectParentMethods(parent, classMap, signatures, visited);
            }
        }
    }

    /**
     * 递归收集类实现的接口的方法（包括继承链上的）
     */
    private void collectAllInterfaceMethods(ParsedClass clazz, Map<String, ParsedClass> classMap,
                                            Set<String> signatures) {
        // 收集当前类的所有父类的接口实现
        Set<String> visited = new HashSet<>();
        collectInterfacesFromHierarchy(clazz, classMap, signatures, visited);
    }

    /**
     * 从类的继承层次中收集所有接口方法
     */
    private void collectInterfacesFromHierarchy(ParsedClass clazz, Map<String, ParsedClass> classMap,
                                                Set<String> signatures, Set<String> visited) {
        if (visited.contains(clazz.getQualifiedName())) {
            return;
        }
        visited.add(clazz.getQualifiedName());

        // 首先检查当前类的implementedInterfaces
        for (String interfaceName : clazz.getImplementedInterfaces()) {
            ParsedClass intf = classMap.get(interfaceName);
            if (intf == null) {
                // 尝试用简单名称查找
                for (ParsedClass pc : classMap.values()) {
                    if (pc.isInterface() && pc.getClassName().equals(interfaceName)) {
                        intf = pc;
                        break;
                    }
                }
            }
            if (intf != null && intf.isInterface()) {
                for (ParsedMethod method : intf.getMethods()) {
                    signatures.add(getMethodSignature(method));
                }
            }
        }

        // 然后检查当前类的所有祖先类是否实现了接口
        // 通过遍历classMap找到所有实现接口的类
        for (ParsedClass potentialInterface : classMap.values()) {
            if (!potentialInterface.isInterface()) {
                continue;
            }
            String intfName = potentialInterface.getClassName(); // 使用简单名称

            // 检查当前类及其父类是否实现了这个接口
            if (doesClassImplementInterface(clazz, intfName, classMap)) {
                for (ParsedMethod method : potentialInterface.getMethods()) {
                    signatures.add(getMethodSignature(method));
                }
            }
        }

        // 继续从父类收集
        String superClass = clazz.getSuperClass();
        if (superClass != null && !superClass.isBlank()) {
            ParsedClass parent = classMap.get(superClass);
            if (parent != null) {
                collectInterfacesFromHierarchy(parent, classMap, signatures, visited);
            }
        }
    }

    /**
     * 检查clazz或其父类是否实现了指定的接口
     */
    private boolean doesClassImplementInterface(ParsedClass clazz, String interfaceName, Map<String, ParsedClass> classMap) {
        Set<String> visited = new HashSet<>();
        return checkImplementInterface(clazz, interfaceName, classMap, visited);
    }

    private boolean checkImplementInterface(ParsedClass clazz, String interfaceName, Map<String, ParsedClass> classMap, Set<String> visited) {
        if (visited.contains(clazz.getQualifiedName())) {
            return false;
        }
        visited.add(clazz.getQualifiedName());

        // 检查当前类的implementedInterfaces
        for (String implIntf : clazz.getImplementedInterfaces()) {
            // 使用简单名称比较
            String implSimpleName = implIntf.contains(".") ? implIntf.substring(implIntf.lastIndexOf('.') + 1) : implIntf;
            if (implSimpleName.equals(interfaceName) || implIntf.equals(interfaceName)) {
                return true;
            }
        }

        // 检查父类
        String superClass = clazz.getSuperClass();
        if (superClass != null && !superClass.isBlank()) {
            ParsedClass parent = classMap.get(superClass);
            if (parent != null) {
                if (checkImplementInterface(parent, interfaceName, classMap, visited)) {
                    return true;
                }
            }
        }

        return false;
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

    private ParsedClass parseClass(String pkg, ClassOrInterfaceDeclaration declaration) {
        ParsedClass parsedClass = new ParsedClass();
        parsedClass.setPackageName(pkg);
        parsedClass.setClassName(declaration.getNameAsString());
        parsedClass.setQualifiedName(pkg.isBlank() ? declaration.getNameAsString() : pkg + "." + declaration.getNameAsString());
        parsedClass.setInterface(declaration.isInterface());
        parsedClass.setSuperClass(resolveSuperClass(declaration));

        // 统计注解数量
        parsedClass.setAnnotationCount(declaration.getAnnotations().size());

        // 收集父类的方法签名（用于检测重写）
        Map<String, String> parentMethodSignatures = collectParentMethodSignatures(declaration);

        Set<String> fields = new HashSet<>();
        Set<String> fieldTypes = new HashSet<>();
        for (FieldDeclaration field : declaration.getFields()) {
            field.getVariables().forEach(v -> fields.add(v.getNameAsString()));
            String fieldType = normalizeType(field.getCommonType().asString());
            fieldTypes.add(fieldType);
            parsedClass.getCoupledTypes().add(fieldType);
        }
        parsedClass.setFieldNames(fields);
        parsedClass.setFieldTypes(fieldTypes);

        declaration.getImplementedTypes().forEach(type -> {
            String normalizedType = normalizeType(type.getNameAsString());
            parsedClass.getCoupledTypes().add(normalizedType);
            parsedClass.getImplementedInterfaces().add(normalizedType);
        });
        declaration.getExtendedTypes().forEach(type -> parsedClass.getCoupledTypes().add(normalizeType(type.getNameAsString())));

        // 先解析所有方法
        List<ParsedMethod> methods = new ArrayList<>();
        for (MethodDeclaration method : declaration.getMethods()) {
            methods.add(parseMethod(parsedClass, method, parentMethodSignatures));
        }

        // 计算重载方法数量
        calculateOverloadCounts(methods);

        for (ParsedMethod pm : methods) {
            parsedClass.getMethods().add(pm);
        }

        // 处理耦合类型
        for (MethodDeclaration method : declaration.getMethods()) {
            method.getTypeParameters().forEach(tp -> parsedClass.getCoupledTypes().add(tp.getNameAsString()));
            method.getParameters().forEach(p -> parsedClass.getCoupledTypes().add(normalizeType(p.getType().asString())));
            parsedClass.getCoupledTypes().add(normalizeType(method.getType().asString()));
            method.findAll(ClassOrInterfaceType.class)
                    .forEach(t -> parsedClass.getCoupledTypes().add(normalizeType(t.getNameAsString())));
        }

        parsedClass.getCoupledTypes().remove(parsedClass.getClassName());
        parsedClass.getCoupledTypes().remove(parsedClass.getQualifiedName());
        parsedClass.getCoupledTypes().removeIf(this::isPrimitiveOrTrivialType);
        return parsedClass;
    }

    /**
     * 收集父类/父接口的方法签名
     * 方法签名 = 方法名 + 参数类型列表
     */
    private Map<String, String> collectParentMethodSignatures(ClassOrInterfaceDeclaration declaration) {
        Map<String, String> signatures = new HashMap<>();

        // 如果有父类，需要获取父类的方法（这里简化处理，假设重写方法的签名相同）
        if (declaration.getExtendedTypes().size() > 0) {
            String superClassName = declaration.getExtendedTypes().get(0).getNameAsString();
            // 标记需要检查重写
            signatures.put("__parent__", superClassName);
        }

        return signatures;
    }

    private ParsedMethod parseMethod(ParsedClass ownerClass, MethodDeclaration method,
                                     Map<String, String> parentMethodSignatures) {
        ParsedMethod parsedMethod = new ParsedMethod();
        parsedMethod.setClassQualifiedName(ownerClass.getQualifiedName());
        parsedMethod.setMethodName(method.getNameAsString());
        parsedMethod.setComplexity(complexityCalculator.calculate(method));

        // 访问修饰符
        parsedMethod.setStatic(method.isStatic());
        parsedMethod.setPrivate(method.isPrivate());
        parsedMethod.setFinal(method.isFinal());

        // 返回类型
        parsedMethod.setReturnType(normalizeType(method.getType().asString()));

        // 参数类型
        List<String> paramTypes = new ArrayList<>();
        for (Parameter param : method.getParameters()) {
            paramTypes.add(normalizeType(param.getType().asString()));
        }
        parsedMethod.setParameterTypes(paramTypes);

        // 检测重写：非static、非private的方法，且有父类签名
        // 初始设置为false，在detectOverriddenMethods中会正确设置
        parsedMethod.setOverridden(false);

        int loc = method.getRange()
                .map(r -> r.end.line - r.begin.line + 1)
                .orElse(0);
        parsedMethod.setLoc(loc);

        Set<String> referencedFields = new HashSet<>();
        method.findAll(NameExpr.class).forEach(nameExpr -> {
            String name = nameExpr.getNameAsString();
            if (ownerClass.getFieldNames().contains(name)) {
                referencedFields.add(name);
            }
        });
        parsedMethod.setReferencedFields(referencedFields);

        Set<String> calls = new HashSet<>();
        method.findAll(MethodCallExpr.class).forEach(call -> calls.add(call.getNameAsString()));
        parsedMethod.setCalledMethods(calls);

        return parsedMethod;
    }

    /**
     * 计算类的重载方法数量（NOO）
     * 教学定义：对于每组同名方法，重载数 = 该组方法数 - 1
     * 例如：add(int,int) 和 add(double,double) 是一组，重载数 = 2 - 1 = 1
     */
    private void calculateOverloadCounts(List<ParsedMethod> methods) {
        // 按方法名分组
        Map<String, List<ParsedMethod>> methodsByName = new HashMap<>();
        for (ParsedMethod method : methods) {
            methodsByName.computeIfAbsent(method.getMethodName(), k -> new ArrayList<>()).add(method);
        }

        // 对于每组同名方法，计算重载数量 = 该组方法数 - 1
        // 然后将这个值设置到每个方法上（方便后续求和）
        for (Map.Entry<String, List<ParsedMethod>> entry : methodsByName.entrySet()) {
            List<ParsedMethod> sameNameMethods = entry.getValue();
            if (sameNameMethods.size() > 1) {
                // 有重载，重载数 = 方法数 - 1
                int overloadCount = sameNameMethods.size() - 1;
                for (ParsedMethod method : sameNameMethods) {
                    method.setOverloadCount(overloadCount);
                }
            }
        }
    }

    /**
     * 判断两个方法是否具有相同的签名（方法名+参数类型）
     */
    private boolean isSameSignature(ParsedMethod m1, ParsedMethod m2) {
        if (!m1.getMethodName().equals(m2.getMethodName())) {
            return false;
        }
        List<String> p1 = m1.getParameterTypes();
        List<String> p2 = m2.getParameterTypes();
        if (p1.size() != p2.size()) {
            return false;
        }
        for (int i = 0; i < p1.size(); i++) {
            if (!p1.get(i).equals(p2.get(i))) {
                return false;
            }
        }
        return true;
    }

    private String resolveSuperClass(ClassOrInterfaceDeclaration declaration) {
        Optional<ClassOrInterfaceType> superType = declaration.getExtendedTypes().stream().findFirst();
        return superType.map(type -> normalizeType(type.getNameAsString())).orElse(null);
    }

    private String normalizeType(String typeName) {
        return typeName.replace("[]", "")
                .replace("<", " ")
                .trim()
                .split("\\s+")[0];
    }

    private boolean isPrimitiveOrTrivialType(String name) {
        return name == null
                || name.isBlank()
                || switch (name) {
                    case "void", "int", "long", "short", "byte", "boolean", "char", "float", "double", "String", "Object", "Class" -> true;
                    default -> false;
                };
    }
}
