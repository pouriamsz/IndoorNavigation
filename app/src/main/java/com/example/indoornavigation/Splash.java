package com.example.indoornavigation;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class Splash extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        boolean checkList = checkLocationPermissions();
        int delay;
        if (checkList){
            delay = 3000;
        }else{
            delay = 1200;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(Splash.this, MainActivity.class));
                finish();
                overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_left);

            }
        },delay);
    }

    // get permissions
    private boolean checkLocationPermissions() {
        int fineLocPrms = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseLocPrms = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION);
        int internetPrms = ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET);
        int networkStat = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_NETWORK_STATE);
        int wifiStat = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_WIFI_STATE);
        int changeWifi = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CHANGE_WIFI_STATE);
        int externalWrite = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int externalRead = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);


        List<String> listPermission = new ArrayList<>();
        if (fineLocPrms != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (coarseLocPrms != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (networkStat != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(android.Manifest.permission.ACCESS_NETWORK_STATE);
        }
        if (internetPrms != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(android.Manifest.permission.INTERNET);
        }
        if (wifiStat != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(android.Manifest.permission.ACCESS_WIFI_STATE);
        }
        if (changeWifi != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(android.Manifest.permission.CHANGE_WIFI_STATE);
        }
        if (externalWrite != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (externalRead != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!listPermission.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermission.toArray(new String[listPermission.size()]), 1);
        }


        return listPermission.size()>0;
    }
}
