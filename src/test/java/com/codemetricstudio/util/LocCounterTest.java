package com.codemetricstudio.util;

import com.codemetricstudio.model.LocMetrics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocCounterTest {

    @Test
    void shouldCountLocByType() {
        List<String> lines = List.of(
                "package demo;",
                "",
                "// single line comment",
                "class A {",
                "  /* block",
                "   comment */",
                "  int x;",
                "}"
        );

        LocMetrics loc = LocCounter.countLines(lines);

        assertEquals(8, loc.getTotalLines());
        assertEquals(1, loc.getBlankLines());
        assertEquals(3, loc.getCommentLines());
        assertEquals(4, loc.getCodeLines());
    }
}
