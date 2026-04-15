package com.codemetricstudio.aggregator;

import com.codemetricstudio.config.ThresholdConfig;
import com.codemetricstudio.model.ParsedClass;
import com.codemetricstudio.model.ParsedMethod;
import com.codemetricstudio.model.ParsedProject;
import com.codemetricstudio.model.ProjectMetrics;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsAggregatorTest {

    @Test
    void shouldAggregateBasicCkMetrics() {
        ParsedClass base = new ParsedClass();
        base.setPackageName("demo");
        base.setClassName("Base");
        base.setQualifiedName("demo.Base");

        ParsedMethod baseMethod = new ParsedMethod();
        baseMethod.setMethodName("baseMethod");
        baseMethod.setComplexity(3);
        baseMethod.setLoc(5);
        baseMethod.setCalledMethods(Set.of("helper"));
        baseMethod.setReferencedFields(Set.of("a"));
        base.getMethods().add(baseMethod);
        base.getFieldNames().add("a");
        base.getCoupledTypes().add("List");

        ParsedClass child = new ParsedClass();
        child.setPackageName("demo");
        child.setClassName("Child");
        child.setQualifiedName("demo.Child");
        child.setSuperClass("Base");

        ParsedProject project = new ParsedProject();
        project.setProjectName("demo");
        project.setFileCount(1);
        project.getClasses().add(base);
        project.getClasses().add(child);

        ProjectMetrics metrics = new MetricsAggregator().aggregate(project, new ThresholdConfig());

        assertEquals(2, metrics.getClassCount());
        assertEquals(1, metrics.getMethodCount());
        assertEquals(3, metrics.getClasses().stream().filter(c -> c.getClassName().equals("demo.Base")).findFirst().orElseThrow().getWmc());
        assertEquals(1, metrics.getClasses().stream().filter(c -> c.getClassName().equals("demo.Base")).findFirst().orElseThrow().getNoc());
        assertEquals(1, metrics.getClasses().stream().filter(c -> c.getClassName().equals("demo.Child")).findFirst().orElseThrow().getDit());
    }
}
