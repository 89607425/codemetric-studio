package com.codemetricstudio.web.model;

public class FunctionPointInput {
    private int ei;
    private int eo;
    private int eq;
    private int ilf;
    private int eif;
    private double vaf = 1.0;

    public int getEi() {
        return ei;
    }

    public void setEi(int ei) {
        this.ei = ei;
    }

    public int getEo() {
        return eo;
    }

    public void setEo(int eo) {
        this.eo = eo;
    }

    public int getEq() {
        return eq;
    }

    public void setEq(int eq) {
        this.eq = eq;
    }

    public int getIlf() {
        return ilf;
    }

    public void setIlf(int ilf) {
        this.ilf = ilf;
    }

    public int getEif() {
        return eif;
    }

    public void setEif(int eif) {
        this.eif = eif;
    }

    public double getVaf() {
        return vaf;
    }

    public void setVaf(double vaf) {
        this.vaf = vaf;
    }
}
