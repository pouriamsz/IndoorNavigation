package com.example.indoornavigation.model;

import java.util.ArrayList;

public class Route {
    ArrayList<Point> vertices;
    int routeSize = 0;

    public Route(ArrayList<Point> vertices){
        this.vertices = vertices;
    }

    public void setPoints(ArrayList<Point> vertices) {
        this.vertices = vertices;
        this.routeSize = vertices.size();
    }

    public void addPoint(Point p){
        this.vertices.add(p);
        this.routeSize+=1;
    }

    public ArrayList<Point> getPoints() {
        return this.vertices;
    }

    public int size(){
        return this.vertices.size();
    }

    public int findNear(Point p){
        int minIndex = 0;
        double minValue = vertices.get(0).distance(p);
        for (int i = 1; i < vertices.size() ; i++) {
            double d = vertices.get(i).distance(p);
            if (d<minValue){
                minValue = d;
                minIndex = i;
            }
        }

        return minIndex;
    }

    public int next(int i){
        this.routeSize-=1;
        return i+1;
    }

    public boolean finish(int i){
        if (i+1 >= vertices.size()){
            return true;
        }

        return false;
    }

    public int getSize(){
        return this.routeSize;
    }
}
