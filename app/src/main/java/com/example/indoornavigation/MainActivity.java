package com.example.indoornavigation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

public class MainActivity extends AppCompatActivity implements SensorEventListener {

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
    int selectedDes = 0;
    AutoCompleteTextView autoCompleteTextView;
    ArrayAdapter<String> arrayAdapter;

    // current position
    Button updateCurrentBtn;
    Point currentPosition = new Point(0,0);
    int currentPoint = 0;
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

    // Sensor
    float alpha = 0.8f; //TODO

    boolean registered = false;
    int firstCheck = 0;
    int ignoreCnt = 0, gyroNotChangingCnt = 0;

    private Sensor accelerometerSensor;
    private float[] accelerometerData = new float[3];
    private static final double MIN_STEP_THRESHOLD = 9.6; //TODO: 9.4 // Minimum value for peak detection
    private static final double STEP_THRESHOLD = 10.7; //TODO: 10.5 // Threshold for peak detection
    private boolean isPeak = false;
    int stepCount = 0;
    boolean getAccels = false, getMags = false;

    private float Rot[] = null; //for gravity rotational data
    private float I[] = null; //for magnetic rotational data
    private float accels[] = new float[3];
    private float mags[] = new float[3];
    private float[] values = new float[3];
    private float yaw, originYaw = 345;
    private ArrayList<Float> yaws = new ArrayList<>();

    private float pitch;
    private float roll;
    private SensorManager sensorManager;
    private Sensor rotationSensor, gyroscopeSensor;
    float gyroY = 0;

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

        //sensor manager & sensor required to calculate yaw
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        registerSensors();

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
                if (currentPoint!=0 && selectedDes!=0){
                    route = dijkstra(graph.getGraph(), currentPoint-1, selectedDes-1);
                    String routeString = "";
                    for (Integer i: route) {
                        routeString += i+"-";
                    }
                    testTxt.setText("" + routeString);
                }
            }
        });

        openCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    double height = Double.parseDouble(edtHeight.getText().toString());
                    stepLength = calculateStrideLength(height);
                    if (route != null && route.size()>0){
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ArrayList<Point> points = new ArrayList<>();
                                for (int ri: route) {
                                    points.add(graph.getPoints().get(ri));
                                }
                                Intent intent = new Intent(MainActivity.this, ARActivity.class);
                                 intent.putExtra("route", points);
                                 intent.putExtra("stepLength", stepLength);
                                startActivity(intent);
                                finish();
                                overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_left);
                            }
                        },600);
                    }

                } catch (Exception ex) {
                    Toast.makeText(getApplicationContext(), "Empty inputs, fill and try again", Toast.LENGTH_SHORT).show();
                }


            }
        });

    }

    private void unregisterSensors() {
        sensorManager.unregisterListener(this);
    }

    private void registerSensors() {
        if (accelerometerSensor!=null){
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        if (gyroscopeSensor!=null){
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        registered = true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (firstCheck<10){
            firstCheck++;
            return;
        }

        gyroscope(event);
        getSensorData(event);
        rotation();

    }


    private void rotation() {
        // Rotation
        if (getMags && getAccels) {

            Rot = new float[9];
            I= new float[9];
            SensorManager.getRotationMatrix(Rot, I, accels, mags);
            float[] outR = new float[9];
            SensorManager.remapCoordinateSystem(Rot, SensorManager.AXIS_X,SensorManager.AXIS_Z, outR);
            SensorManager.getOrientation(outR, values);

            // here we calculated the final yaw(azimuth), roll & pitch of the device.
            // multiplied by a global standard value to get accurate results

            double mAzimuthAngleNotFlat = Math.toDegrees(Math
                    .atan2((outR[1] - outR[3]), (outR[0] + outR[4])));

            mAzimuthAngleNotFlat += 180;
            // this is the yaw or the azimuth we need
            modifyYaw((float)mAzimuthAngleNotFlat);
            pitch = (float)Math.toDegrees(values[1]);
            roll = (float)Math.toDegrees(values[2]);

            getAccels = false;
            getMags = false;
        }
    }


    private void getSensorData(SensorEvent event) {
        switch (event.sensor.getType())
        {
            case Sensor.TYPE_MAGNETIC_FIELD:
                mags[0] = alpha*mags[0] + (1-alpha)*event.values[0];
                mags[1] = alpha*mags[1] + (1-alpha)*event.values[1];
                mags[2] = alpha*mags[2] + (1-alpha)*event.values[2];
                getMags = true;
                break;
            case Sensor.TYPE_ACCELEROMETER:
                accelerometerData[0] = event.values[0];
                accelerometerData[1] = event.values[1];
                accelerometerData[2] = event.values[2];

                accels[0] = alpha*accels[0] + (1-alpha)*event.values[0];
                accels[1] = alpha*accels[1] + (1-alpha)*event.values[1];
                accels[2] = alpha*accels[2] + (1-alpha)*event.values[2];
                getAccels = true;
                break;
        }
    }


    private void gyroscope(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            float rotationX = event.values[0];
            gyroY = event.values[1];
            float rotationZ = event.values[2];
//            txtSteps.setText( "Yaw = " + yaw+"\n"+
//                    "steps = " + stepCount + "\n"+
//                    "gyro x = " + rotationX + "\n" +
//                    "gyro y = " + rotationY + "\n" +
//                    "gyro z = " + rotationZ + "\n"
//            );
        }

    }


    private void modifyYaw(float value) {
        if (yaws.size() >= 3 && Math.abs(gyroY) < 0.05){ // TODO
            if (gyroNotChangingCnt >= 15){
                return;
            }
            yaw = 0;
            for (int i = 0; i < yaws.size(); i++) {
                yaw += yaws.get(i);
            }
            yaw = yaw/yaws.size();
            if (ignoreCnt<=5 && Math.abs(yaw - value )>10 && gyroY<0.1) { //TODO
                ignoreCnt++;
            }else if (ignoreCnt > 5){ // TODO
                ignoreCnt--; // TODO
                yaws.remove(0);
                yaws.add(value);
                yaw = 0;
                for (int i = 0; i < yaws.size(); i++) {
                    yaw += yaws.get(i);
                }
                yaw = yaw/yaws.size();
            }else{
                if (yaws.size()>3){ //TODO
                    yaws.remove(0);
                }
                yaws.add(value);
                yaw = 0;
                for (int i = 0; i < yaws.size(); i++) {
                    yaw += yaws.get(i);
                }
                yaw /= yaws.size();
            }
            gyroNotChangingCnt++;
            return;
        }
        if (Math.abs(gyroY)>0.5){
            yaws = new ArrayList<>();
            ignoreCnt = 0;
            gyroNotChangingCnt=0;
        }
        if ((yaw > 340 && value < 20) || (yaw<20 && value>340)){
            yaws = new ArrayList<>();
        }
        if (yaws.size()==0){
            yaw = value;
            yaws.add(yaw);
        }else{
            yaw = 0;
            for (int i = 0; i < yaws.size(); i++) {
                yaw += yaws.get(i);
            }
            yaw = yaw/yaws.size();
            if (ignoreCnt<=5 && Math.abs(yaw - value )>10 && gyroY<0.1) { //TODO
                ignoreCnt++;
            }else if (ignoreCnt > 5){ // TODO
                ignoreCnt--; // TODO
                yaws.remove(0);
                yaws.add(value);
                yaw = 0;
                for (int i = 0; i < yaws.size(); i++) {
                    yaw += yaws.get(i);
                }
                yaw = yaw/yaws.size();
            }else{
                if (yaws.size()>3){ //TODO
                    yaws.remove(0);
                }
                yaws.add(value);
                yaw = 0;
                for (int i = 0; i < yaws.size(); i++) {
                    yaw += yaws.get(i);
                }
                yaw /= yaws.size();
            }

        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    public double calculateStrideLength(double heightInMeter) {
        double heightPercentage = 0.3;
        if (male){
            heightPercentage = 0.23;
        }else{
            heightPercentage = 0.20;
        }

        double strideLength = heightInMeter * heightPercentage;

        return strideLength;
    }


    private void graphPoints() {
        graph.addPoint(new Point(0.6, 0.6)); // 1
        graph.addPoint(new Point(0.6, 2.1)); // 2
        graph.addPoint(new Point(0.6, 3.6)); // 3
        graph.addPoint(new Point(0.6, 5.1)); // 4
        graph.addPoint(new Point(0.6, 6.6)); // 5
        graph.addPoint(new Point(0.6, 8.1)); // 6
        graph.addPoint(new Point(0.6, 11.1)); // 7
        graph.addPoint(new Point(0.6, 14.1)); // 8
        graph.addPoint(new Point(0.6, 17.1)); // 9
        graph.addPoint(new Point(0.6, 20.1)); // 10
        graph.addPoint(new Point(0.0, 27.3)); // 11
        graph.addPoint(new Point(3.0, 27.3)); // 12
        graph.addPoint(new Point(0.6, 27.3)); // 13
        graph.addPoint(new Point(2.1, 0.6)); // 14
        graph.addPoint(new Point(3.6, 0.6)); // 15
        graph.addPoint(new Point(5.1, 0.6)); // 16
        graph.addPoint(new Point(8.1, 0.6)); // 17
        graph.addPoint(new Point(2.1, 2.1)); // 18
        graph.addPoint(new Point(2.1, 3.6)); // 19
        graph.addPoint(new Point(3.6, 3.6)); // 20
        graph.addPoint(new Point(3.6, 2.1)); // 21
        graph.addPoint(new Point(5.1, 2.1)); // 22
        graph.addPoint(new Point(5.1, 3.6)); // 23
        graph.addPoint(new Point(6.6, 3.6)); // 24
        graph.addPoint(new Point(6.6, 2.1)); // 25
        graph.addPoint(new Point(8.1, 2.1)); // 26
        graph.addPoint(new Point(8.1, 3.6)); // 27
        graph.addPoint(new Point(9.6, 2.1)); // 28
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
                "y : " + currentPosition.getY() + "\n" +
                "xp : " + sp.getX() + "\n" +
                "yp : " + sp.getY());
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
            if (yaw>230 && yaw <300){
                pointsToSearch.add(fp.getPoints().get(3));

                // down
            }else if (yaw>130 && yaw< 200){
                pointsToSearch.add(fp.getPoints().get(2));

                // right
            }else if (yaw>30 && yaw<100){
                pointsToSearch.add(fp.getPoints().get(1));

                // up
            }else if ((yaw>300 && yaw <360) || (yaw>0 && yaw<30)){
                pointsToSearch.add(fp.getPoints().get(0));
            }else{
                pointsToSearch.add(fp.getPoints().get(0));
                pointsToSearch.add(fp.getPoints().get(1));
                pointsToSearch.add(fp.getPoints().get(2));
                pointsToSearch.add(fp.getPoints().get(3));

            }
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
        sensorManager.unregisterListener(this);
        registered = false;
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
