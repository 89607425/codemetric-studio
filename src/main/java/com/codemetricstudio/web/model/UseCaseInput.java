package com.codemetricstudio.web.model;

public class UseCaseInput {
    private int actorSimple;
    private int actorAverage;
    private int actorComplex;
    private int ucSimple;
    private int ucAverage;
    private int ucComplex;
    private double tcf = 1.0;
    private double ecf = 1.0;

    public int getActorSimple() {
        return actorSimple;
    }

    public void setActorSimple(int actorSimple) {
        this.actorSimple = actorSimple;
    }

    public int getActorAverage() {
        return actorAverage;
    }

    public void setActorAverage(int actorAverage) {
        this.actorAverage = actorAverage;
    }

    public int getActorComplex() {
        return actorComplex;
    }

    public void setActorComplex(int actorComplex) {
        this.actorComplex = actorComplex;
    }

    public int getUcSimple() {
        return ucSimple;
    }

    public void setUcSimple(int ucSimple) {
        this.ucSimple = ucSimple;
    }

    public int getUcAverage() {
        return ucAverage;
    }

    public void setUcAverage(int ucAverage) {
        this.ucAverage = ucAverage;
    }

    public int getUcComplex() {
        return ucComplex;
    }

    public void setUcComplex(int ucComplex) {
        this.ucComplex = ucComplex;
    }

    public double getTcf() {
        return tcf;
    }

    public void setTcf(double tcf) {
        this.tcf = tcf;
    }

    public double getEcf() {
        return ecf;
    }

    public void setEcf(double ecf) {
        this.ecf = ecf;
    }
}
