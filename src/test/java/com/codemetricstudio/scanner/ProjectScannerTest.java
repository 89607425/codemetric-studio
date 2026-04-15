package com.codemetricstudio.scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectScannerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSkipBuildTargetAndGitDirectories() throws IOException {
        Files.createDirectories(tempDir.resolve("src/main/java"));
        Files.createDirectories(tempDir.resolve("target/generated"));
        Files.createDirectories(tempDir.resolve("build/generated"));
        Files.createDirectories(tempDir.resolve(".git/hooks"));

        Files.writeString(tempDir.resolve("src/main/java/App.java"), "class App {}");
        Files.writeString(tempDir.resolve("target/generated/Skip.java"), "class Skip {}");
        Files.writeString(tempDir.resolve("build/generated/Skip2.java"), "class Skip2 {}");
        Files.writeString(tempDir.resolve(".git/hooks/Skip3.java"), "class Skip3 {}");

        List<Path> files = new ProjectScanner().scan(tempDir, null);

        assertEquals(1, files.size());
    }
}
