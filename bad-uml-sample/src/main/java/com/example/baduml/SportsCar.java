package com.example.baduml;

public class SportsCar extends Car {
    private Turbo turbo;

    public SportsCar() {
        super();
    }

    @Override
    public void accelerate() {
        turbo.engage();
        super.accelerate();
    }

    @Override
    public void accelerate(int delta) {
        turbo.engage();
        super.accelerate(delta * 2);
    }

    public void accelerate(Turbo t) {
        this.turbo = t;
        t.engage();
    }

    public void drift() {
        System.out.println("Drifting!");
    }
}
