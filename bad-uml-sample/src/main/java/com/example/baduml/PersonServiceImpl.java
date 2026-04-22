package com.example.baduml;

import java.util.ArrayList;
import java.util.List;

public class PersonServiceImpl implements PersonService {
    protected final List<String> logs = new ArrayList<>();

    @Override
    public void updateInfo() {
        int score = 0;
        for (int i = 0; i < 8; i++) {
            if (i % 2 == 0) {
                score += i;
            } else {
                score -= i;
            }
            if (i > 5 && i < 7) {
                logs.add("risk-mid");
            } else if (i >= 7) {
                logs.add("risk-high");
            } else {
                logs.add("risk-low");
            }
        }

        if (score > 12) {
            logs.add("A");
        } else if (score > 6) {
            logs.add("B");
        } else if (score > 0) {
            logs.add("C");
        } else {
            logs.add("D");
        }
    }
}
