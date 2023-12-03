package com.example.indoornavigation;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.indoornavigation.model.FPoint;
import com.example.indoornavigation.model.FingerPrint;
import com.example.indoornavigation.model.Graph;
import com.example.indoornavigation.model.NearFP;
import com.example.indoornavigation.model.Point;
import com.example.indoornavigation.model.Route;
import com.example.indoornavigation.model.Router;
import com.example.indoornavigation.model.SampleFPoint;
import com.example.indoornavigation.model.SampleRouter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView testTxt;
    private EditText edtHeight;
    RadioButton rMale, rFemale;
    boolean male = true;

    // destination
    String[] destinationList = {"آز سنجش از دور مایکروویو",
            "آز ژئودزی ماهواره ای",
            "عملیات فتو 1",
            "آز گرانی سنجی",
            "دفتر مقام معظم رهبری",
            "دکتر فاطمه جمشید پور ",
            "اتاق شورا",
            "آز فتوگرامتری پهپاد",
            "بایگانی",
            "آز خدمات مکان مبنا",
    };
    int selectedDes = 4;
    AutoCompleteTextView autoCompleteTextView;
    ArrayAdapter<String> arrayAdapter;

    // current position
    Button updateCurrentBtn;
    Point currentPosition;
    int currentPoint;
    private ArrayList<FingerPrint> fingerPrints = new ArrayList<>();
    private ArrayList<Router> routers = new ArrayList<>();
    private ArrayList<SampleRouter> srs = new ArrayList<>();

    // WIFI
    private WifiManager wifiManager;
    private boolean reScan = true;

    // Route
    Button directionBtn;
    private static final int INF = Integer.MAX_VALUE;
    Graph graph = new Graph(28, 28);
    List<Integer> route = new ArrayList<>();

    // Camera
    Button openCamera;
    double stepLength = 0.5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testTxt = findViewById(R.id.testTxt);
        directionBtn = findViewById(R.id.directionBtn);
        openCamera = findViewById(R.id.openCamera);
        rMale = findViewById(R.id.radioMale);
        rFemale = findViewById(R.id.radioFemale);
        edtHeight = findViewById(R.id.edtHeight);

        rMale.setOnCheckedChangeListener((buttonView, isChecked) -> male = isChecked);

        rFemale.setOnCheckedChangeListener((buttonView, isChecked) -> male = !isChecked);

        // Load Finger Prints
        for (int i = 1; i < 3; i++) {
            String fileName = "hawx" + i + ".json";
            getFingerPrints(fileName);
        }

        // WIFI
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "Wifi is disabled. You need to enable it", Toast.LENGTH_SHORT).show();
            wifiManager.setWifiEnabled(true);
        }

        autoCompleteTextView = findViewById(R.id.autoCompleteText);
        arrayAdapter = new ArrayAdapter<>(this, R.layout.destination_list, destinationList);

        autoCompleteTextView.setAdapter(arrayAdapter);

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                getDestination(position);
            }
        });

        updateCurrentBtn = findViewById(R.id.updateCurrent);
        updateCurrentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCurrentPosition();
            }
        });


        graphPoints();
        graphConnections();

        directionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                route = dijkstra(graph.getGraph(), currentPoint, selectedDes);
                String routeString = "";
                for (Integer i: route) {
                    routeString += i+"-";
                }
                testTxt.setText("" + routeString);
            }
        });

        openCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    double height = Double.parseDouble(edtHeight.getText().toString());
                    stepLength = calculateStrideLength(height);

                } catch (Exception ex) {
                    Toast.makeText(getApplicationContext(), "Empty inputs, fill and try again", Toast.LENGTH_SHORT).show();
                }

                if (route != null && route.size()>0){
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ArrayList<Point> points = graph.getPoints();
                            Intent intent = new Intent(MainActivity.this, ARActivity.class);
                            intent.putExtra("route", points);
                            intent.putExtra("stepLength", stepLength);
                            startActivity(intent);
                            finish();
                            overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_left);
                        }
                    },600);
                }
            }
        });

    }


    public double calculateStrideLength(double heightInMeter) {
        double heightPercentage = 0.3;
        if (male){
            heightPercentage = 0.3;
        }else{
            heightPercentage = 0.28;
        }

        double strideLength = heightInMeter * heightPercentage;

        return strideLength;
    }


    private void graphPoints() {
        graph.addPoint(new Point(0.6, 0.6));
        graph.addPoint(new Point(0.6, 2.1));
        graph.addPoint(new Point(0.6, 3.6));
        graph.addPoint(new Point(0.6, 5.1));
        graph.addPoint(new Point(0.6, 6.6));
        graph.addPoint(new Point(0.6, 8.1));
        graph.addPoint(new Point(0.6, 11.1));
        graph.addPoint(new Point(0.6, 14.1));
        graph.addPoint(new Point(0.6, 17.1));
        graph.addPoint(new Point(0.6, 20.1));
        graph.addPoint(new Point(0.0, 27.3));
        graph.addPoint(new Point(3.0, 27.3));
        graph.addPoint(new Point(0.6, 27.3));
        graph.addPoint(new Point(2.1, 0.6));
        graph.addPoint(new Point(3.6, 0.6));
        graph.addPoint(new Point(5.1, 0.6));
        graph.addPoint(new Point(8.1, 0.6));
        graph.addPoint(new Point(2.1, 2.1));
        graph.addPoint(new Point(2.1, 3.6));
        graph.addPoint(new Point(3.6, 3.6));
        graph.addPoint(new Point(3.6, 2.1));
        graph.addPoint(new Point(5.1, 2.1));
        graph.addPoint(new Point(5.1, 3.6));
        graph.addPoint(new Point(6.6, 3.6));
        graph.addPoint(new Point(6.6, 2.1));
        graph.addPoint(new Point(8.1, 2.1));
        graph.addPoint(new Point(8.1, 3.6));
        graph.addPoint(new Point(9.6, 2.1));
    }


    public static List<Integer> dijkstra(int[][] graph, int startNode, int endNode) {
        int n = graph.length;
        int[] dist = new int[n];
        int[] prev = new int[n];
        boolean[] visited = new boolean[n];

        Arrays.fill(dist, INF);
        Arrays.fill(prev, -1);

        dist[startNode] = 0;

        for (int i = 0; i < n - 1; i++) {
            int u = minDistance(dist, visited);
            visited[u] = true;

            for (int v = 0; v < n; v++) {
                if (!visited[v] && graph[u][v] != 0 && dist[u] != INF
                        && dist[u] + graph[u][v] < dist[v]) {
                    dist[v] = dist[u] + graph[u][v];
                    prev[v] = u;
                }
            }
        }

        return reconstructRoute(prev, startNode, endNode);
    }

    private static int minDistance(int[] dist, boolean[] visited) {
        int min = INF, minIndex = -1;

        for (int v = 0; v < dist.length; v++) {
            if (!visited[v] && dist[v] <= min) {
                min = dist[v];
                minIndex = v;
            }
        }

        return minIndex;
    }

    private static List<Integer> reconstructRoute(int[] prev, int start, int end) {
        List<Integer> route = new ArrayList<>();
        for (int at = end; at != -1; at = prev[at]) {
            route.add(at);
        }
        Collections.reverse(route);
        return route;
    }

    private void graphConnections() {
        graph.connect(1,new int[]{2,14});
        graph.connect(2,new int[]{1,3, 18});
        graph.connect(3,new int[]{2,4, 19});
        graph.connect(4,new int[]{3,5});
        graph.connect(5,new int[]{4,6});
        graph.connect(6,new int[]{5,7});
        graph.connect(7,new int[]{6,8});
        graph.connect(8,new int[]{7,9});
        graph.connect(9,new int[]{8,10});
        graph.connect(10,new int[]{9, 13});
        graph.connect(11,new int[]{13});
        graph.connect(12,new int[]{13});
        graph.connect(13,new int[]{11, 12, 10});
        graph.connect(14,new int[]{1, 18, 15});
        graph.connect(15,new int[]{14, 16, 21});
        graph.connect(16,new int[]{15, 22});
        graph.connect(17,new int[]{26});
        graph.connect(18,new int[]{2, 14, 19, 21});
        graph.connect(19,new int[]{3, 18, 20});
        graph.connect(20,new int[]{19, 21, 23});
        graph.connect(21,new int[]{18, 20, 22, 15});
        graph.connect(22,new int[]{21, 23, 25, 16});
        graph.connect(23,new int[]{20, 22, 24});
        graph.connect(24,new int[]{23, 25, 27});
        graph.connect(25,new int[]{22, 24, 26});
        graph.connect(26,new int[]{17, 25, 27, 28});
        graph.connect(27,new int[]{24, 26});
        graph.connect(28,new int[]{26});
    }

    private void getDestination(int position) {
        switch (position){
            case 0:
                selectedDes = 4;
                break;
            case 1:
                selectedDes = 6;
                break;
            case 2:
                selectedDes = 7;
                break;
            case 3:
                selectedDes = 8;
                break;
            case 4:
                selectedDes = 9;
                break;
            case 5:
                selectedDes = 10;
                break;
            case 6:
                selectedDes = 11;
                break;
            case 7:
                selectedDes = 12;
                break;
            case 8:
                selectedDes = 17;
                break;
            case 9:
                selectedDes = 28;
                break;
        }
    }

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
        testTxt.setText( "n : " + currentPoint +"\n"+
                "x : " + currentPosition.getX()+ "\n"+
                "y : " + currentPosition.getY());
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
            for (FPoint p : fp.getPoints()) {
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


}
