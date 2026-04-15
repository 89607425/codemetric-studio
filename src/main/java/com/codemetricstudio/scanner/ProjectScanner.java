package com.codemetricstudio.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectScanner {

    public List<Path> scan(Path projectRoot, String module) throws IOException {
        Path basePath = module == null || module.isBlank() ? projectRoot : projectRoot.resolve(module).normalize();
        if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
            throw new IllegalArgumentException("Path does not exist or is not a directory: " + basePath);
        }

        try (Stream<Path> stream = Files.walk(basePath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !isExcluded(path))
                    .collect(Collectors.toList());
        }
    }

    private boolean isExcluded(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return normalized.contains("/target/")
                || normalized.contains("/build/")
                || normalized.contains("/.git/");
    }
}
