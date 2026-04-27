package com.codemetricstudio.metrics;

import com.codemetricstudio.model.LocMetrics;
import com.codemetricstudio.model.ParsedClass;
import com.codemetricstudio.model.ParsedProject;
import com.codemetricstudio.parser.JavaAstParser;
import com.codemetricstudio.util.LocCounter;
import com.codemetricstudio.web.model.SourceFilePayload;

import java.util.ArrayList;
import java.util.List;

public class InMemoryProjectAnalyzer {

    private final JavaAstParser parser = new JavaAstParser();

    public ParsedProject analyze(String projectName, List<SourceFilePayload> files, String module) {
        ParsedProject project = new ParsedProject();
        project.setProjectName(projectName == null || projectName.isBlank() ? "uploaded-project" : projectName);

        String modulePrefix = module == null ? "" : module.replace('\\', '/').strip();
        LocMetrics projectLoc = new LocMetrics(0, 0, 0, 0);
        List<ParsedClass> classes = new ArrayList<>();
        int javaFileCount = 0;

        for (SourceFilePayload file : files) {
            String path = file.getPath() == null ? "" : file.getPath().replace('\\', '/');
            String content = file.getContent() == null ? "" : file.getContent();

            if (!path.endsWith(".java") || isExcluded(path)) {
                continue;
            }
            if (!modulePrefix.isBlank() && !path.startsWith(modulePrefix)) {
                continue;
            }

            javaFileCount++;
            projectLoc.add(LocCounter.countLines(linesOf(content)));
            classes.addAll(parser.parseSource(content));
        }

        parser.detectOverriddenMethods(classes);
        project.setFileCount(javaFileCount);
        project.setLoc(projectLoc);
        project.setClasses(classes);
        return project;
    }

    private List<String> linesOf(String content) {
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        return List.of(normalized.split("\\n", -1));
    }

    private boolean isExcluded(String path) {
        return path.contains("/target/") || path.contains("/build/") || path.contains("/.git/");
    }
}
