package com.codemetricstudio.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ThresholdConfigLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ThresholdConfigLoader() {
    }

    public static ThresholdConfig load(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return new ThresholdConfig();
        }
        return MAPPER.readValue(path.toFile(), ThresholdConfig.class);
    }
}
