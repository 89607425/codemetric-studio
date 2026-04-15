package com.codemetricstudio.metrics;

import com.codemetricstudio.model.ParsedClass;
import com.codemetricstudio.model.ParsedMethod;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LcomCalculatorTest {

    @Test
    void shouldReturnZeroForSingleMethod() {
        ParsedClass clazz = new ParsedClass();
        ParsedMethod method = new ParsedMethod();
        method.setReferencedFields(Set.of("a"));
        clazz.getMethods().add(method);

        assertEquals(0.0, new LcomCalculator().calculate(clazz));
    }

    @Test
    void shouldCalculatePairBasedLcom() {
        ParsedClass clazz = new ParsedClass();

        ParsedMethod m1 = new ParsedMethod();
        m1.setReferencedFields(Set.of("a"));
        ParsedMethod m2 = new ParsedMethod();
        m2.setReferencedFields(Set.of("b"));
        ParsedMethod m3 = new ParsedMethod();
        m3.setReferencedFields(Set.of("a"));

        clazz.getMethods().add(m1);
        clazz.getMethods().add(m2);
        clazz.getMethods().add(m3);

        assertEquals(1.0, new LcomCalculator().calculate(clazz));
    }
}
