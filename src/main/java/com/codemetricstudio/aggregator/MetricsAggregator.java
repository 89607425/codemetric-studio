package com.codemetricstudio.aggregator;

import com.codemetricstudio.config.ThresholdConfig;
import com.codemetricstudio.metrics.ExtendedMetricsCalculator;
import com.codemetricstudio.metrics.LcomCalculator;
import com.codemetricstudio.metrics.PolymorphismCalculator;
import com.codemetricstudio.model.Alert;
import com.codemetricstudio.model.ClassMetrics;
import com.codemetricstudio.model.MethodMetrics;
import com.codemetricstudio.model.ParsedClass;
import com.codemetricstudio.model.ParsedMethod;
import com.codemetricstudio.model.ParsedProject;
import com.codemetricstudio.model.PolymorphismMetrics;
import com.codemetricstudio.model.ProjectMetrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MetricsAggregator {

    private final LcomCalculator lcomCalculator = new LcomCalculator();
    private final PolymorphismCalculator polymorphismCalculator = new PolymorphismCalculator();
    private final ExtendedMetricsCalculator extendedMetricsCalculator = new ExtendedMetricsCalculator();

    public ProjectMetrics aggregate(ParsedProject parsedProject, ThresholdConfig thresholdConfig) {
        ProjectMetrics metrics = new ProjectMetrics();
        metrics.setProjectName(parsedProject.getProjectName());
        metrics.setFileCount(parsedProject.getFileCount());
        metrics.setLoc(parsedProject.getLoc());

        Map<String, ParsedClass> classMap = new HashMap<>();
        parsedProject.getClasses().forEach(pc -> classMap.put(pc.getQualifiedName(), pc));

        Map<String, Integer> nocByClass = buildNocMap(parsedProject.getClasses(), classMap);
        Set<String> projectMethodNames = buildProjectMethodNameSet(parsedProject.getClasses());

        List<ClassMetrics> classMetricsList = new ArrayList<>();
        List<MethodMetrics> methodMetricsList = new ArrayList<>();
        List<Alert> alerts = new ArrayList<>();

        for (ParsedClass parsedClass : parsedProject.getClasses()) {
            ClassMetrics classMetrics = new ClassMetrics();
            classMetrics.setPackageName(parsedClass.getPackageName());
            classMetrics.setClassName(parsedClass.getQualifiedName());

            // ===== CK 核心度量 =====
            // Teaching-caliber CK口径: WMC按类中定义的方法个数统计
            classMetrics.setWmc(parsedClass.getMethods().size());
            classMetrics.setDit(calculateDit(parsedClass, classMap));
            classMetrics.setNoc(nocByClass.getOrDefault(parsedClass.getQualifiedName(), 0));
            classMetrics.setCbo(parsedClass.getCoupledTypes().size());
            classMetrics.setRfc(calculateRfc(parsedClass, projectMethodNames));
            classMetrics.setLcom(lcomCalculator.calculate(parsedClass));

            // ===== 多态性度量 =====
            ParsedClass parentClass = findBySimpleOrQualifiedName(parsedClass.getSuperClass(), classMap);
            PolymorphismMetrics polyMetrics = polymorphismCalculator.calculate(parsedClass, parentClass);
            classMetrics.setNop(polyMetrics.getNop());
            classMetrics.setNom(polyMetrics.getNom());
            classMetrics.setNoo(polyMetrics.getNoo());
            classMetrics.setPod(polyMetrics.getPod());
            classMetrics.setOverrideRatio(polyMetrics.getOverrideRatio());
            classMetrics.setOverloadRatio(polyMetrics.getOverloadRatio());

            // ===== 扩展度量 =====
            var extMetrics = extendedMetricsCalculator.calculate(
                    parsedClass,
                    classMetrics.getDit(),
                    classMetrics.getNoc(),
                    parentClass,
                    parsedProject.getClasses()
            );
            classMetrics.setSk(extMetrics.getSk());
            classMetrics.setDac(extMetrics.getDac());
            classMetrics.setMoa(extMetrics.getMoa());
            classMetrics.setMfa(extMetrics.getMfa());
            classMetrics.setCam(extMetrics.getCam());
            classMetrics.setCis(extMetrics.getCis());
            classMetrics.setNsc(extMetrics.getNsc());
            classMetrics.setCoa(extMetrics.getCoa());
            classMetrics.setSize1(extMetrics.getSize1());
            classMetrics.setMpc(extMetrics.getMpc());
            classMetrics.setAif(extMetrics.getAif());
            classMetrics.setMif(extMetrics.getMif());

            classMetricsList.add(classMetrics);

            // 告警检查
            if (classMetrics.getWmc() > thresholdConfig.getClassWmc()) {
                alerts.add(new Alert(
                        "WMC",
                        parsedClass.getQualifiedName(),
                        classMetrics.getWmc(),
                        thresholdConfig.getClassWmc(),
                        "Split class responsibilities and extract complex methods."
                ));
            }
            if (classMetrics.getCbo() > thresholdConfig.getClassCbo()) {
                alerts.add(new Alert(
                        "CBO",
                        parsedClass.getQualifiedName(),
                        classMetrics.getCbo(),
                        thresholdConfig.getClassCbo(),
                        "Reduce direct dependencies and introduce interfaces/facades."
                ));
            }

            // 多态性告警
            if (classMetrics.getPod() > 0.9) {
                alerts.add(new Alert(
                        "HIGH_POLYMORPHISM",
                        parsedClass.getQualifiedName(),
                        (int)(classMetrics.getPod() * 100),
                        90,
                        "Very high polymorphism degree. Ensure proper override contracts are maintained."
                ));
            }

            for (ParsedMethod parsedMethod : parsedClass.getMethods()) {
                MethodMetrics methodMetrics = new MethodMetrics(
                        parsedClass.getQualifiedName(),
                        parsedMethod.getMethodName(),
                        parsedMethod.getComplexity(),
                        parsedMethod.getLoc()
                );
                methodMetricsList.add(methodMetrics);

                if (parsedMethod.getComplexity() > thresholdConfig.getMethodComplexity()) {
                    alerts.add(new Alert(
                            "COMPLEXITY",
                            parsedClass.getQualifiedName() + "#" + parsedMethod.getMethodName(),
                            parsedMethod.getComplexity(),
                            thresholdConfig.getMethodComplexity(),
                            "Refactor conditional branches and split method into smaller units."
                    ));
                }
            }
        }

        metrics.setClassCount(classMetricsList.size());
        metrics.setMethodCount(methodMetricsList.size());
        metrics.setClasses(classMetricsList);
        metrics.setMethods(methodMetricsList);
        metrics.setAlerts(alerts);
        return metrics;
    }

    private int calculateRfc(ParsedClass parsedClass, Set<String> projectMethodNames) {
        Set<String> ownMethodNames = new HashSet<>();
        parsedClass.getMethods().forEach(method -> ownMethodNames.add(method.getMethodName()));

        Set<String> externalProjectCalls = new HashSet<>();
        parsedClass.getMethods().forEach(method -> {
            method.getCalledMethods().stream()
                    .filter(projectMethodNames::contains)
                    .filter(called -> !ownMethodNames.contains(called))
                    .forEach(externalProjectCalls::add);
        });
        return ownMethodNames.size() + externalProjectCalls.size();
    }

    private Set<String> buildProjectMethodNameSet(List<ParsedClass> classes) {
        Set<String> names = new HashSet<>();
        for (ParsedClass parsedClass : classes) {
            for (ParsedMethod method : parsedClass.getMethods()) {
                names.add(method.getMethodName());
            }
        }
        return names;
    }

    private int calculateDit(ParsedClass parsedClass, Map<String, ParsedClass> classMap) {
        int depth = 0;
        String currentSuper = parsedClass.getSuperClass();

        while (currentSuper != null && !currentSuper.isBlank()) {
            depth++;
            ParsedClass next = findBySimpleOrQualifiedName(currentSuper, classMap);
            if (next == null) {
                break;
            }
            currentSuper = next.getSuperClass();
        }
        return depth;
    }

    private Map<String, Integer> buildNocMap(List<ParsedClass> classes, Map<String, ParsedClass> classMap) {
        Map<String, Integer> result = new HashMap<>();
        for (ParsedClass clazz : classes) {
            String superClass = clazz.getSuperClass();
            if (superClass == null || superClass.isBlank()) {
                continue;
            }
            ParsedClass parent = findBySimpleOrQualifiedName(superClass, classMap);
            if (parent != null) {
                result.merge(parent.getQualifiedName(), 1, Integer::sum);
            }
        }
        return result;
    }

    private ParsedClass findBySimpleOrQualifiedName(String name, Map<String, ParsedClass> classMap) {
        if (name == null || name.isBlank()) {
            return null;
        }
        ParsedClass byQualified = classMap.get(name);
        if (byQualified != null) {
            return byQualified;
        }
        return classMap.values().stream()
                .filter(pc -> pc.getClassName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
