package com.codemetricstudio.model;

public class Alert {
    private String type;
    private String target;
    private int value;
    private int threshold;
    private String suggestion;

    public Alert() {
    }

    public Alert(String type, String target, int value, int threshold, String suggestion) {
        this.type = type;
        this.target = target;
        this.value = value;
        this.threshold = threshold;
        this.suggestion = suggestion;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
}
