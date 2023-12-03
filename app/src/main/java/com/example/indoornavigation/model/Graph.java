package com.example.indoornavigation.model;

import java.util.ArrayList;

public class Graph {
    private int[][] graph;
    private ArrayList<Point> points = new ArrayList<>();

    public Graph(int nx, int ny){
        graph = new int[nx][ny];
    }

    public void addPoint(Point p){
            this.points.add(p);
    }

    public Point getPoint(int i){
        return this.points.get(i);
    }

    public ArrayList<Point> getPoints(){
        return this.points;
    }

    public void connect(int i, int[] js){
        for (int j: js) {
            this.graph[i-1][j-1] = 1;
        }
    }

    public int[][] getGraph() {
        return graph;
    }
}
