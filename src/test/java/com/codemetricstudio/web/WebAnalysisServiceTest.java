package com.codemetricstudio.web;

import com.codemetricstudio.web.model.FunctionPointInput;
import com.codemetricstudio.web.model.SourceFilePayload;
import com.codemetricstudio.web.model.UseCaseInput;
import com.codemetricstudio.web.model.WebAnalyzeRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebAnalysisServiceTest {

    @Test
    void shouldReturnProjectAndDesignMetrics() {
        SourceFilePayload javaFile = new SourceFilePayload();
        javaFile.setPath("src/main/java/demo/B.java");
        javaFile.setContent("""
                package demo;
                class B {
                  int work(int i) { if (i > 0 && i < 10) return i; return 0; }
                }
                """);

        FunctionPointInput fp = new FunctionPointInput();
        fp.setEi(2);
        fp.setEo(1);
        fp.setEq(1);
        fp.setIlf(1);
        fp.setEif(0);
        fp.setVaf(1.0);

        UseCaseInput uc = new UseCaseInput();
        uc.setActorSimple(1);
        uc.setActorAverage(1);
        uc.setActorComplex(0);
        uc.setUcSimple(2);
        uc.setUcAverage(1);
        uc.setUcComplex(0);
        uc.setTcf(1.0);
        uc.setEcf(1.0);

        WebAnalyzeRequest req = new WebAnalyzeRequest();
        req.setProjectName("demo");
        req.setSourceFiles(List.of(javaFile));
        req.setFunctionPointInput(fp);
        req.setUseCaseInput(uc);
        req.setCfgText("if(a){for(;;){}} && b");

        var resp = new WebAnalysisService().analyze(req);

        assertEquals("demo", resp.getProjectMetrics().getProjectName());
        assertEquals(1, resp.getProjectMetrics().getFileCount());
        assertEquals(27, resp.getDesignMetrics().getUfp());
        assertEquals(23, resp.getDesignMetrics().getUucp());
        assertEquals(4, resp.getDesignMetrics().getCfgCyclomaticComplexity());
    }
}
