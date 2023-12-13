package com.example.indoornavigation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.indoornavigation.model.FPoint;
import com.example.indoornavigation.model.FingerPrint;
import com.example.indoornavigation.model.NearFP;
import com.example.indoornavigation.model.Point;
import com.example.indoornavigation.model.Router;
import com.example.indoornavigation.model.SampleFPoint;
import com.example.indoornavigation.model.SampleRouter;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class InitPosition extends AppCompatActivity {

    Button scanQRBtn, updateCurrentBtn, nextStepBtn;
    EditText initPositionEdt;


    // Current position
    Point currentPosition = new Point(0,0);
    int currentPoint = 0;
    private ArrayList<FingerPrint> fingerPrints = new ArrayList<>();
    private ArrayList<Router> routers = new ArrayList<>();
    private ArrayList<SampleRouter> srs = new ArrayList<>();

    // WIFI
    private WifiManager wifiManager;
    private boolean reScan = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.init_position);

        scanQRBtn = findViewById(R.id.scanQRBtn);
        updateCurrentBtn = findViewById(R.id.updateCurrent);
        nextStepBtn = findViewById(R.id.nextStepBtn);
        initPositionEdt = findViewById(R.id.edtInitPosition);


        double stepLength = (double) getIntent().getDoubleExtra("stepLength", 0.5);


        // Load Finger Prints
        for (int i = 1; i < 3; i++) {
            String fileName = "hawx" + i + ".json";
            getFingerPrints(fileName);
        }

        scanQRBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanQR();
            }
        });

        updateCurrentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCurrentPosition();
            }
        });

        nextStepBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!initPositionEdt.getText().toString().equals("")){

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(InitPosition.this, MainActivity.class);
                            intent.putExtra("currentIdx", currentPoint);
                            intent.putExtra("stepLength", stepLength);
                            startActivity(intent);
                            finish();
                            overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_left);
                        }
                    },600);

                }else{
                    Toast.makeText(getApplicationContext(), "Please update your current position", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private void scanQR() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Volume up to flash on");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }

    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {

        if (result.getContents()!=null){
            currentPoint = Integer.valueOf(result.getContents());
            initPositionEdt.setText("" + currentPoint);
        }

    });

    private void updateCurrentPosition() {
        srs.clear();
        registerReceiver(wifiScanReceiver, new IntentFilter(wifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }

    BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            boolean success = intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED, false);
            unregisterReceiver(this);

            if (success) {
                scanSuccess();
            } else {
                // scan failure handling
                if (reScan) {
                    scanFailure();
                } else {
                    scanSuccess();
                }
            }
        }
    };


    private void scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        List<ScanResult> results = wifiManager.getScanResults();
        reScan = false;
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                updateCurrentPosition();
            }
        }, 5000);
    }

    private void scanSuccess() {
        SampleRouter r = null;
        SampleFPoint sp = new SampleFPoint();
        reScan = true;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        List<ScanResult> results = wifiManager.getScanResults();
        for (ScanResult scanResult : results) {
            r = new SampleRouter(scanResult.SSID, scanResult.BSSID);
            r.setRSSI(scanResult.level * -1);
            srs.add(r);
        }
        sp.setWifiList(srs);
        srs.clear();
        FingerPrint fpMin = findPoint(sp);
        // TODO: check sp
        // use sp to detect current position and find the nearest point in finger prints to sp
        currentPosition.setX(fpMin.getX());
        currentPosition.setY(fpMin.getY());
        currentPoint = fpMin.getNumber();
        initPositionEdt.setText( "n : " + currentPoint);
    }


    // read json string and create finger prints list
    private void getFingerPrints(String fileName) {
        ArrayList<FPoint> points = new ArrayList<>();


        try {
            JSONObject obj = new JSONObject(loadJsonFromAssets(fileName));
            JSONArray fingerPrintsJA = obj.getJSONArray("finger_prints");

            for (int i = 0; i < fingerPrintsJA.length(); i++) {
                JSONObject fpObject = fingerPrintsJA.getJSONObject(i);
                Integer n = Integer.parseInt(fpObject.getString("n"));
                Double x = Double.parseDouble(fpObject.getString("x"));
                Double y = Double.parseDouble(fpObject.getString("y"));
                FingerPrint fp = new FingerPrint(n, x, y);

                JSONArray pointsJA = fpObject.getJSONArray("points");
                for (int j = 0; j < pointsJA.length(); j++) {
                    JSONObject pObject = pointsJA.getJSONObject(j);
                    String dir = pObject.getString("dir");
                    float yaw =  (float)pObject.getDouble("yaw");
                    FPoint p = new FPoint(dir, yaw);

                    JSONArray routersJA = pObject.getJSONArray("routers");
                    for (int k = 0; k < routersJA.length(); k++) {
                        JSONObject rObject = routersJA.getJSONObject(k);
                        String SSID = rObject.getString("SSID");
                        String BSSID = rObject.getString("BSSID");
                        Double meanRSSI = Double.parseDouble(rObject.getString("meanRSSI"));
                        Router r = new Router(SSID, BSSID);
                        r.setMeanRSSI(meanRSSI);

                        JSONArray rssiJA = rObject.getJSONArray("RSSI");
                        for (int l = 0; l < rssiJA.length(); l++) {
                            r.addRSSI(rssiJA.getInt(l));
                        }

                        routers.add(r);
                    }

                    p.setWifiList(routers);
                    routers.clear();
                    points.add(p);
                }
                fp.setPoints(points);
                fingerPrints.add(fp);

                points.clear();

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    // load json file as string
    private String loadJsonFromAssets(String fileName) {
        String jsonString;
        try {
            InputStream is = getApplicationContext().getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jsonString = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return jsonString;
    }


    // find nearest point and calculate position
    // according to k nearest point
    private FingerPrint findPoint(SampleFPoint sp_) {
        ArrayList<Double> deltaRSSI = new ArrayList<>();
        ArrayList<Double> rmseForEachDir = new ArrayList<>();
        HashMap<Double, FingerPrint> fpRMSE;
        fpRMSE = new HashMap<Double, FingerPrint>();

        HashMap<Double, FingerPrint> knnFPs;
        knnFPs = new HashMap<Double, FingerPrint>();


        Double fpMinVal = 10e4;
        FingerPrint fpMin = new FingerPrint(0, 0.0, 0.0);
        boolean foundRouter = false;




//        ArrayList<NearFP> nfps = new ArrayList<>();

        // beine tamame finger print ha loop mizanim
        // va har 4 point ke dar har finger print darim baresi mikonim
        // har point dar direction haye motafavet (u-r-d-l)
        // shamele router haye motafavet hastan
        for (FingerPrint fp : fingerPrints
        ) {
            ArrayList<FPoint> pointsToSearch = new ArrayList<>();

            // left
//            if (yaw>230 && yaw <300){
//                pointsToSearch.add(fp.getPoints().get(3));
//
//                // down
//            }else if (yaw>130 && yaw< 200){
//                pointsToSearch.add(fp.getPoints().get(2));
//
//                // right
//            }else if (yaw>30 && yaw<100){
//                pointsToSearch.add(fp.getPoints().get(1));
//
//                // up
//            }else if ((yaw>300 && yaw <360) || (yaw>0 && yaw<30)){
//                pointsToSearch.add(fp.getPoints().get(0));
//            }else{
//                pointsToSearch.add(fp.getPoints().get(0));
//                pointsToSearch.add(fp.getPoints().get(1));
//                pointsToSearch.add(fp.getPoints().get(2));
//                pointsToSearch.add(fp.getPoints().get(3));
//
//            }

            pointsToSearch.add(fp.getPoints().get(0));

            for (FPoint p : pointsToSearch) {
                // inja router haye noghte morede nazar ra
                // ba router haye point p barresi mikonim
                for (SampleRouter sr : sp_.getWifiList()
                ) {
                    for (Router r : p.getWifiList()
                    ) {
                        // dar soorate moshtarak budan BSSID
                        // ekhtelap RSSI point morede nazar va mean RSSI p dar fp
                        // mohasebe mishavad
                        if (r.getBSSID().equals(sr.getBSSID())) {

                            Double deltaVal = Math.abs(r.getMeanRSSI() - sr.getRSSI());
                            deltaRSSI.add(deltaVal);
                            foundRouter = true;
                            break;
                        }

                    }
                    // dar soorati ke BSSID mojud nabud
                    // khode RSSI vare ezafe mishavad
                    if (!foundRouter) {
                        deltaRSSI.add(Double.parseDouble("" + sr.getRSSI()));
                    }
                    foundRouter = false;

                }
                // hasel jame deltaRSSI hesab mishavad
                Double sum = 0.0;
                for (Double d : deltaRSSI
                ) {
                    sum += (d * d);
                }
                // aval az RMSE estefade shod vali bad tasmim gereftim az MSE estefade konim
                // name variable hamchenan RMSE baghi mande ast
                Double RMSE = Math.sqrt(sum);
                deltaRSSI.clear();

                rmseForEachDir.add(RMSE);

            }
            Double foundedMinRMSE = findMinRMSE(rmseForEachDir);


            fpRMSE.put(foundedMinRMSE, fp);

            // peida kardan finger printi ke daraye kamtarin MSE ast
            rmseForEachDir.clear();
            if (fpMinVal > foundedMinRMSE) {
                fpMin = fp;
                fpMinVal = foundedMinRMSE;

            }

        }

        // sort kardan finger print ha bar asase MSE ha
        // baraye peida kardn k point nazdik
        ArrayList<Double> rmseValsForSort = new ArrayList<>(fpRMSE.keySet());
        Collections.sort(rmseValsForSort);

        ArrayList<FingerPrint> fpForFindMin = new ArrayList<>();

        // dar soorati ke Router haye moshtarake kamtari az k dashte basahim
        // in shart check mishavad
        // ta az crash kardan app jelogiri shavad
        int kInKNN = 3;

        if (fpMin.getNumber()>=4 && fpMin.getNumber()<=13){
            kInKNN = 1;
        }
        if (rmseValsForSort.size() > kInKNN) {
            for (int i = 0; i < kInKNN; i++) {
                knnFPs.put(rmseValsForSort.get(i), fpRMSE.get(rmseValsForSort.get(i)));
                fpForFindMin.add(fpRMSE.get(rmseValsForSort.get(i)));
            }
        } else {
            for (int i = 0; i < rmseValsForSort.size(); i++) {
                knnFPs.put(rmseValsForSort.get(i), fpRMSE.get(rmseValsForSort.get(i)));
                fpForFindMin.add(fpRMSE.get(rmseValsForSort.get(i)));
            }
        }

        // another approach to find nearest finger print
//        nfps = findNearestPoint(fpForFindMin, sp);

        calcPointCoordinate(sp_, knnFPs);


        return fpMin;
    }


    // another approach to find nearest finger print
    // but it didn't use
    private ArrayList<NearFP> findNearestPoint(ArrayList<FingerPrint> fps, SampleFPoint sp) {
        ArrayList<NearFP> nfps = new ArrayList<>();
        NearFP nfp;
        boolean findNFP = false;

        for (FingerPrint fp : fps
        ) {
            for (FPoint p : fp.getPoints()) {
                for (SampleRouter sr : sp.getWifiList()
                ) {
                    for (Router r : p.getWifiList()
                    ) {
                        if (r.getBSSID().equals(sr.getBSSID())) {

                            Double deltaVal = Math.abs(r.getMeanRSSI() - sr.getRSSI());
                            for (NearFP n : nfps
                            ) {
                                if (n.getBssid().equals(sr.getBSSID())) {

                                    if (n.getMinVal() > deltaVal) {
                                        n.setMinVal(deltaVal);
                                        n.setNumber(fp.getNumber());
                                    }
                                    findNFP = true;
                                    break;
                                }
                            }
                            if (!findNFP) {
                                nfp = new NearFP();
                                nfp.setBssid(sr.getBSSID());
                                nfp.setNumber(fp.getNumber());
                                nfp.setMinVal(Math.abs(r.getMeanRSSI() - sr.getRSSI()));
                                nfps.add(nfp);
                            }
                            findNFP = false;
                            break;
                        }

                    }

                }

            }

        }

        return nfps;
    }

    // calculate point coordinate according to k nearest neighbour
    private void calcPointCoordinate(SampleFPoint sp, HashMap<Double, FingerPrint> fps) {
        Double coordX = 0.0;
        Double coordY = 0.0;
        Double sumWeights = 0.0;

        for (Double d : fps.keySet()) {
            coordX += ((1 / d) * fps.get(d).getX());
            coordY += ((1 / d) * fps.get(d).getY());
            sumWeights += (1 / d);
        }
        coordX /= sumWeights;
        coordY /= sumWeights;
        sp.setX(coordX);
        sp.setY(coordY);

    }

    // find min MSE between 4 direction (u-r-d-l)
    private Double findMinRMSE(ArrayList<Double> rmseFED) {
        Double min = rmseFED.get(0);

        for (int i = 1; i < rmseFED.size(); i++) {
            if (min > rmseFED.get(i)) {
                min = rmseFED.get(i);
            }
        }

        return min;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
