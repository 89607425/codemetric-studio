package com.example.baduml;

public class Engine {
    private String type;
    private int horsepower;

    public void start() {
        this.type = "V8";
        this.horsepower = 500;
    }

    public void stop() {
        this.type = null;
        this.horsepower = 0;
    }
}
