package com.codemetricstudio.metrics;

import com.codemetricstudio.model.ParsedClass;
import com.codemetricstudio.model.ParsedMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LcomCalculator {

    public double calculate(ParsedClass parsedClass) {
        List<ParsedMethod> methods = new ArrayList<>(parsedClass.getMethods());
        if (methods.size() < 2) {
            return 0.0;
        }

        int p = 0;
        int q = 0;
        for (int i = 0; i < methods.size(); i++) {
            for (int j = i + 1; j < methods.size(); j++) {
                Set<String> a = methods.get(i).getReferencedFields();
                Set<String> b = methods.get(j).getReferencedFields();
                boolean sharesField = a.stream().anyMatch(b::contains);
                if (sharesField) {
                    q++;
                } else {
                    p++;
                }
            }
        }
        return Math.max(p - q, 0);
    }
}
