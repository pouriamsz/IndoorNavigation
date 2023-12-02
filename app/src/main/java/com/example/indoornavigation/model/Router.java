package com.example.indoornavigation.model;


import java.util.ArrayList;

public class Router {
    private String SSID, BSSID;
    private ArrayList<Integer> RSSI;
    private Double meanRSSI;

    public Router(String SSID, String BSSID) {
        this.SSID = SSID;
        this.BSSID = BSSID;
        this.RSSI = new ArrayList<Integer>();
        this.meanRSSI = 0.0;
    }

    public ArrayList<Integer> getRSSI() {
        return RSSI;
    }

    public Double getMeanRSSI() {
        return meanRSSI;
    }

    public String getBSSID() {
        return BSSID;
    }

    public String getSSID() {
        return SSID;
    }

    public void addRSSI(Integer RSSI) {
        this.RSSI.add(RSSI);
    }

    public void calcMeanRSSI() {
        Double sumRSSI = 0.0;
        for (int i = 0; i < this.RSSI.size(); i++) {
            sumRSSI += this.RSSI.get(i);
        }
        this.meanRSSI = sumRSSI / this.RSSI.size();
    }

    public void setMeanRSSI(Double meanRSSI){
        this.meanRSSI = meanRSSI;
    }
}

