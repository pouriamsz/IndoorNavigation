package com.example.indoornavigation.model;

import java.util.ArrayList;

public class FPoint {
    private String dir;
    private float yaw;
    private ArrayList<Router> wifiList;

    public FPoint(String dir, float yaw) {
        this.dir = dir;
        this.yaw = yaw;
        this.wifiList = new ArrayList<>();
    }

    public String getDir() {
        return dir;
    }

    public ArrayList<Router> getWifiList() {
        return wifiList;
    }

    public void setWifiList(ArrayList<Router> wifiList) {
        for (Router r : wifiList) {
            this.wifiList.add(r);
        }
    }
}
