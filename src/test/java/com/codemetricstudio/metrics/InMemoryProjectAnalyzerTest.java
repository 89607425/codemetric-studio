package com.codemetricstudio.metrics;

import com.codemetricstudio.model.ParsedProject;
import com.codemetricstudio.web.model.SourceFilePayload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryProjectAnalyzerTest {

    @Test
    void shouldAnalyzeUploadedJavaSources() {
        SourceFilePayload javaFile = new SourceFilePayload();
        javaFile.setPath("src/main/java/demo/A.java");
        javaFile.setContent("""
                package demo;
                class A {
                  int x;
                  int add(int a, int b) { if (a > 0) { return a + b; } return b; }
                }
                """);

        SourceFilePayload ignored = new SourceFilePayload();
        ignored.setPath("README.md");
        ignored.setContent("# demo");

        ParsedProject project = new InMemoryProjectAnalyzer().analyze("demo", List.of(javaFile, ignored), null);

        assertEquals(1, project.getFileCount());
        assertEquals(1, project.getClasses().size());
        assertEquals("demo.A", project.getClasses().get(0).getQualifiedName());
    }
}
