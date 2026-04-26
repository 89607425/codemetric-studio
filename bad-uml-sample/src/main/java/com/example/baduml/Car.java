package com.example.baduml;

public class Car {
    private Engine engine;
    private int speed;

    public Car() {
        this.speed = 0;
    }

    public void accelerate() {
        this.speed += 10;
    }

    public void accelerate(int delta) {
        this.speed += delta;
    }

    public void brake() {
        this.speed = 0;
    }

    public int getSpeed() {
        return this.speed;
    }

    public static void honk() {
        System.out.println("Beep!");
    }
}
