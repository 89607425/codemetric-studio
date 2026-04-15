package com.codemetricstudio.util;

import com.codemetricstudio.model.LocMetrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class LocCounter {
    private LocCounter() {
    }

    public static LocMetrics count(Path file) throws IOException {
        return countLines(Files.readAllLines(file));
    }

    public static LocMetrics countLines(List<String> lines) {
        int total = lines.size();
        int blank = 0;
        int comment = 0;
        int code = 0;
        boolean inBlockComment = false;

        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty()) {
                blank++;
                continue;
            }

            if (inBlockComment) {
                comment++;
                if (line.contains("*/")) {
                    inBlockComment = false;
                }
                continue;
            }

            if (line.startsWith("//")) {
                comment++;
                continue;
            }

            if (line.startsWith("/*")) {
                comment++;
                if (!line.contains("*/")) {
                    inBlockComment = true;
                }
                continue;
            }

            if (line.contains("/*")) {
                code++;
                if (!line.contains("*/")) {
                    inBlockComment = true;
                }
                continue;
            }

            code++;
        }

        return new LocMetrics(total, blank, comment, code);
    }
}
