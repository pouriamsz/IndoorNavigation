package com.example.indoornavigation.model;

import java.util.ArrayList;

public class FingerPrint {
    private Integer number;
    private Double x, y;
    private ArrayList<FPoint> points;

    public FingerPrint(Integer number, Double x, Double y) {
        this.number = number;
        this.x = x;
        this.y = y;
        this.points = new ArrayList<>();
    }

    public Double getY() {
        return y;
    }

    public Double getX() {
        return x;
    }

    public Integer getNumber() {
        return number;
    }

    public ArrayList<FPoint> getPoints() {
        return points;
    }

    public void setPoints(ArrayList<FPoint> points) {
        for (FPoint p : points) {
            this.points.add(p);
        }
    }

    public void setY(Double y) {
        this.y = y;
    }

    public void setX(Double x) {
        this.x = x;
    }
}

