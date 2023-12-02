package com.example.indoornavigation.model;

public class Graph {
    private int[][] graph;

    public Graph(int nx, int ny){
        graph = new int[nx][ny];
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
