package com.example.indoornavigation.model;

import java.util.ArrayList;

public class SampleFPoint {

    private Double x,y;
    private ArrayList<SampleRouter> wifiList;

    public SampleFPoint() {
        this.wifiList = new ArrayList<>();
    }

    public void setX(Double x) {
        this.x = x;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public void setWifiList(ArrayList<SampleRouter> wifiList) {
        for (SampleRouter r : wifiList) {
            this.wifiList.add(r);
        }
    }

    public ArrayList<SampleRouter> getWifiList() {
        return wifiList;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }
}
