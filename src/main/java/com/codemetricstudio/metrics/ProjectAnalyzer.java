package com.codemetricstudio.metrics;

import com.codemetricstudio.model.LocMetrics;
import com.codemetricstudio.model.ParsedClass;
import com.codemetricstudio.model.ParsedProject;
import com.codemetricstudio.parser.JavaAstParser;
import com.codemetricstudio.util.LocCounter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ProjectAnalyzer {

    private final JavaAstParser parser = new JavaAstParser();

    public ParsedProject analyze(String projectName, List<Path> files) {
        ParsedProject project = new ParsedProject();
        project.setProjectName(projectName);
        project.setFileCount(files.size());

        LocMetrics projectLoc = new LocMetrics(0, 0, 0, 0);
        List<ParsedClass> classes = new ArrayList<>();

        for (Path file : files) {
            try {
                projectLoc.add(LocCounter.count(file));
            } catch (IOException ignore) {
                // Skip unreadable files to keep analysis resilient.
            }
            classes.addAll(parser.parseFile(file));
        }

        project.setLoc(projectLoc);
        project.setClasses(classes);
        return project;
    }
}
