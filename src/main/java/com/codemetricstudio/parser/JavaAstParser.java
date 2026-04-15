package com.codemetricstudio.parser;

import com.codemetricstudio.metrics.CyclomaticComplexityCalculator;
import com.codemetricstudio.model.ParsedClass;
import com.codemetricstudio.model.ParsedMethod;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class JavaAstParser {

    private final CyclomaticComplexityCalculator complexityCalculator = new CyclomaticComplexityCalculator();

    public JavaAstParser() {
        ParserConfiguration configuration = new ParserConfiguration();
        StaticJavaParser.setConfiguration(configuration);
    }

    public List<ParsedClass> parseFile(Path file) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(Files.readString(file));
            String pkg = cu.getPackageDeclaration().map(pd -> pd.getName().asString()).orElse("");

            List<ParsedClass> classes = new ArrayList<>();
            for (ClassOrInterfaceDeclaration declaration : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                classes.add(parseClass(pkg, declaration));
            }
            return classes;
        } catch (IOException | RuntimeException ex) {
            return List.of();
        }
    }

    private ParsedClass parseClass(String pkg, ClassOrInterfaceDeclaration declaration) {
        ParsedClass parsedClass = new ParsedClass();
        parsedClass.setPackageName(pkg);
        parsedClass.setClassName(declaration.getNameAsString());
        parsedClass.setQualifiedName(pkg.isBlank() ? declaration.getNameAsString() : pkg + "." + declaration.getNameAsString());
        parsedClass.setInterface(declaration.isInterface());
        parsedClass.setSuperClass(resolveSuperClass(declaration));

        Set<String> fields = new HashSet<>();
        for (FieldDeclaration field : declaration.getFields()) {
            field.getVariables().forEach(v -> fields.add(v.getNameAsString()));
            parsedClass.getCoupledTypes().add(normalizeType(field.getCommonType().asString()));
        }
        parsedClass.setFieldNames(fields);

        declaration.getImplementedTypes().forEach(type -> parsedClass.getCoupledTypes().add(normalizeType(type.getNameAsString())));
        declaration.getExtendedTypes().forEach(type -> parsedClass.getCoupledTypes().add(normalizeType(type.getNameAsString())));

        for (MethodDeclaration method : declaration.getMethods()) {
            parsedClass.getMethods().add(parseMethod(parsedClass, method));
            method.getTypeParameters().forEach(tp -> parsedClass.getCoupledTypes().add(tp.getNameAsString()));
            method.getParameters().forEach(p -> parsedClass.getCoupledTypes().add(normalizeType(p.getType().asString())));
            parsedClass.getCoupledTypes().add(normalizeType(method.getType().asString()));
            method.findAll(ClassOrInterfaceType.class)
                    .forEach(t -> parsedClass.getCoupledTypes().add(normalizeType(t.getNameAsString())));
        }

        parsedClass.getCoupledTypes().remove(parsedClass.getClassName());
        parsedClass.getCoupledTypes().remove(parsedClass.getQualifiedName());
        parsedClass.getCoupledTypes().removeIf(this::isPrimitiveOrTrivialType);
        return parsedClass;
    }

    private ParsedMethod parseMethod(ParsedClass ownerClass, MethodDeclaration method) {
        ParsedMethod parsedMethod = new ParsedMethod();
        parsedMethod.setClassQualifiedName(ownerClass.getQualifiedName());
        parsedMethod.setMethodName(method.getNameAsString());
        parsedMethod.setComplexity(complexityCalculator.calculate(method));

        int loc = method.getRange()
                .map(r -> r.end.line - r.begin.line + 1)
                .orElse(0);
        parsedMethod.setLoc(loc);

        Set<String> referencedFields = new HashSet<>();
        method.findAll(NameExpr.class).forEach(nameExpr -> {
            String name = nameExpr.getNameAsString();
            if (ownerClass.getFieldNames().contains(name)) {
                referencedFields.add(name);
            }
        });
        parsedMethod.setReferencedFields(referencedFields);

        Set<String> calls = new HashSet<>();
        method.findAll(MethodCallExpr.class).forEach(call -> calls.add(call.getNameAsString()));
        parsedMethod.setCalledMethods(calls);

        return parsedMethod;
    }

    private String resolveSuperClass(ClassOrInterfaceDeclaration declaration) {
        Optional<ClassOrInterfaceType> superType = declaration.getExtendedTypes().stream().findFirst();
        return superType.map(type -> normalizeType(type.getNameAsString())).orElse(null);
    }

    private String normalizeType(String typeName) {
        return typeName.replace("[]", "")
                .replace("<", " ")
                .trim()
                .split("\\s+")[0];
    }

    private boolean isPrimitiveOrTrivialType(String name) {
        return name == null
                || name.isBlank()
                || switch (name) {
                    case "void", "int", "long", "short", "byte", "boolean", "char", "float", "double", "String", "Object", "Class" -> true;
                    default -> false;
                };
    }
}
