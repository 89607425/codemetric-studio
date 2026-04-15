package com.codemetricstudio.reporter;

import com.codemetricstudio.model.ProjectMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonReporter {

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void write(ProjectMetrics metrics, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        Path out = outputDir.resolve("metrics.json");
        mapper.writeValue(out.toFile(), metrics);
    }
}
