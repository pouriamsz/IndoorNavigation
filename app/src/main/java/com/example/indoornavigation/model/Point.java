package com.example.indoornavigation.model;

public class Point {
    double x;
    double y;
    double z;
    double lat;
    double lon;

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

    // this should be current point
    public Point toCartesianCoordinateNoRotation(Point point) {
        double x = point.x - this.x;
        double y = point.y - this.y;

        return new Point(x, y);
    }

    // Distance by x & y
    public double distance(Point p) {
        return Math.sqrt((this.x - p.x) * (this.x - p.x) +
                (this.y - p.y) * (this.y - p.y) +
                (this.z - p.z) * (this.z - p.z)
        );
    }


}
