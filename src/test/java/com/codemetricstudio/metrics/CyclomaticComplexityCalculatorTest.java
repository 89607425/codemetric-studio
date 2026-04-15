package com.codemetricstudio.metrics;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CyclomaticComplexityCalculatorTest {

    @Test
    void shouldCountDecisionPoints() {
        String methodCode = """
                int sample(int a, int b) {
                    if (a > 0 && b > 0) {
                        for (int i = 0; i < a; i++) {
                            if (i % 2 == 0 || b > 2) {
                                b++;
                            }
                        }
                    } else {
                        while (b > 0) {
                            b--;
                        }
                    }
                    return b;
                }
                """;

        MethodDeclaration method = StaticJavaParser.parseMethodDeclaration(methodCode);
        int complexity = new CyclomaticComplexityCalculator().calculate(method);

        assertEquals(7, complexity);
    }
}
