package com.example.indoornavigation.model;

public class NearFP {
    private String bssid;
    private Integer number;
    private Double minVal;

    public NearFP() {
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public void setMinVal(Double minVal) {
        this.minVal = minVal;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Double getMinVal() {
        return minVal;
    }

    public Integer getNumber() {
        return number;
    }

    public String getBssid() {
        return bssid;
    }
}
