package com.example.indoornavigation.model;

public class Point {
    double x;
    double y;
    double z;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
        this.z = 0.0;
    }

    // Set

    public void setX(double x){this.x=x;}
    public void setY(double y){this.y=y;}
    public void setZ(double z) {
        this.z = z;
    }

    // Get
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }


    // Distance by x & y
    public double distance(Point p) {
        return Math.sqrt((this.x - p.x) * (this.x - p.x) +
                (this.y - p.y) * (this.y - p.y) +
                (this.z - p.z) * (this.z - p.z)
        );
    }


}
