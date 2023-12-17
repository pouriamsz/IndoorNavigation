package com.example.indoornavigation.model;

import java.io.Serializable;

public class Point implements Serializable {
    public double x;
    public double y;
    double z;

    public Point(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
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

    public Point sub(Point p){
        return new Point(this.x - p.getX(), this.y-p.getY(), this.z - p.getZ());
    }

    public double norm() {
        return Math.sqrt(this.norm2());
    }

    public double norm2() {
        return this.x * this.x +
                this.y * this.y +
                this.z * this.z;

    }

    public double length(){
        return this.norm();
    }

    public double dot(Point p1) {
        return (this.x * p1.x) +
                (this.y * p1.y) +
                (this.z * p1.z);
    }

    public double cross(Point p){
        return (this.x*p.getY())-(this.y*p.getX());
    }

}
