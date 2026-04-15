package com.codemetricstudio.aggregator;

import com.codemetricstudio.config.ThresholdConfig;
import com.codemetricstudio.metrics.LcomCalculator;
import com.codemetricstudio.model.Alert;
import com.codemetricstudio.model.ClassMetrics;
import com.codemetricstudio.model.MethodMetrics;
import com.codemetricstudio.model.ParsedClass;
import com.codemetricstudio.model.ParsedMethod;
import com.codemetricstudio.model.ParsedProject;
import com.codemetricstudio.model.ProjectMetrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MetricsAggregator {

    private final LcomCalculator lcomCalculator = new LcomCalculator();

    public ProjectMetrics aggregate(ParsedProject parsedProject, ThresholdConfig thresholdConfig) {
        ProjectMetrics metrics = new ProjectMetrics();
        metrics.setProjectName(parsedProject.getProjectName());
        metrics.setFileCount(parsedProject.getFileCount());
        metrics.setLoc(parsedProject.getLoc());

        Map<String, ParsedClass> classMap = new HashMap<>();
        parsedProject.getClasses().forEach(pc -> classMap.put(pc.getQualifiedName(), pc));

        Map<String, Integer> nocByClass = buildNocMap(parsedProject.getClasses(), classMap);

        List<ClassMetrics> classMetricsList = new ArrayList<>();
        List<MethodMetrics> methodMetricsList = new ArrayList<>();
        List<Alert> alerts = new ArrayList<>();

        for (ParsedClass parsedClass : parsedProject.getClasses()) {
            ClassMetrics classMetrics = new ClassMetrics();
            classMetrics.setPackageName(parsedClass.getPackageName());
            classMetrics.setClassName(parsedClass.getQualifiedName());
            classMetrics.setWmc(parsedClass.getMethods().stream().mapToInt(ParsedMethod::getComplexity).sum());
            classMetrics.setDit(calculateDit(parsedClass, classMap));
            classMetrics.setNoc(nocByClass.getOrDefault(parsedClass.getQualifiedName(), 0));
            classMetrics.setCbo(parsedClass.getCoupledTypes().size());
            classMetrics.setRfc(calculateRfc(parsedClass));
            classMetrics.setLcom(lcomCalculator.calculate(parsedClass));
            classMetricsList.add(classMetrics);

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

    private int calculateRfc(ParsedClass parsedClass) {
        Set<String> responseSet = new HashSet<>();
        parsedClass.getMethods().forEach(method -> {
            responseSet.add(method.getMethodName());
            responseSet.addAll(method.getCalledMethods());
        });
        return responseSet.size();
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
