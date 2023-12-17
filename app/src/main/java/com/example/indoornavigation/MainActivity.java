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
import android.widget.CheckBox;
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

public class MainActivity extends AppCompatActivity {

    TextView testTxt;
    CheckBox blindCheck;
    boolean blindMode = false;

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
    int currentPoint = 0;

    // Route
    Button directionBtn;
    private static final int INF = Integer.MAX_VALUE;
    Graph graph = new Graph(28, 28);
    Graph graphBlind = new Graph(28, 28);

    List<Integer> route = new ArrayList<>();

    // Camera
    Button openCamera;
    double stepLength = 0.5;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentPoint = (int) getIntent().getIntExtra("currentIdx", 1);

        stepLength = (double) getIntent().getDoubleExtra("stepLength", 0.5);

        blindCheck = findViewById(R.id.checkBox);
        testTxt = findViewById(R.id.testTxt);
        directionBtn = findViewById(R.id.directionBtn);
        openCamera = findViewById(R.id.openCamera);


        autoCompleteTextView = findViewById(R.id.autoCompleteText);
        arrayAdapter = new ArrayAdapter<>(this, R.layout.destination_list, destinationList);

        autoCompleteTextView.setAdapter(arrayAdapter);

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                getDestination(position);
            }
        });


        addGraphPoints(graph);
        addGraphPoints(graphBlind);
        graphConnections();
        graphBlindConnections();

        directionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Graph usedGraph;
                if (blindCheck.isChecked()){
                    blindMode = true;
                    usedGraph = graphBlind;
                }else{
                    blindMode = false;
                    usedGraph = graph;
                }
                if (currentPoint!=0 && selectedDes!=0){
                    route = dijkstra(usedGraph.getGraph(), currentPoint-1, selectedDes-1);
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

    private void graphBlindConnections() {
        graphBlind.connect(1,new int[]{2, 14});
        graphBlind.connect(2,new int[]{1,3, 18});
        graphBlind.connect(3,new int[]{2,4, 19});
        graphBlind.connect(4,new int[]{3,5});
        graphBlind.connect(5,new int[]{4,6});
        graphBlind.connect(6,new int[]{5,7});
        graphBlind.connect(7,new int[]{6,8});
        graphBlind.connect(8,new int[]{7,9});
        graphBlind.connect(9,new int[]{8,10});
        graphBlind.connect(10,new int[]{9, 13});
        graphBlind.connect(11,new int[]{13});
        graphBlind.connect(12,new int[]{13});
        graphBlind.connect(13,new int[]{11, 12, 10});
        graphBlind.connect(14,new int[]{1, 15});
        graphBlind.connect(15,new int[]{14, 16});
        graphBlind.connect(16,new int[]{15});
        graphBlind.connect(17,new int[]{26});
        graphBlind.connect(18,new int[]{2, 19});
        graphBlind.connect(19,new int[]{3, 18, 20});
        graphBlind.connect(20,new int[]{19, 21, 23});
        graphBlind.connect(21,new int[]{20});
        graphBlind.connect(22,new int[]{23});
        graphBlind.connect(23,new int[]{20, 22, 24});
        graphBlind.connect(24,new int[]{23, 25, 27});
        graphBlind.connect(25,new int[]{24});
        graphBlind.connect(26,new int[]{17, 27, 28});
        graphBlind.connect(27,new int[]{24, 26});
        graphBlind.connect(28,new int[]{26});
    }

    private void addGraphPoints(Graph _graph) {
        _graph.addPoint(new Point(0.6, 0.6, 0)); // 1
        _graph.addPoint(new Point(0.6, 2.1, 0)); // 2
        _graph.addPoint(new Point(0.6, 3.6, 0)); // 3
        _graph.addPoint(new Point(0.6, 5.1, 0)); // 4
        _graph.addPoint(new Point(0.6, 6.6, 0)); // 5
        _graph.addPoint(new Point(0.6, 8.1, 0)); // 6
        _graph.addPoint(new Point(0.6, 11.1, 0)); // 7
        _graph.addPoint(new Point(0.6, 14.1, 0)); // 8
        _graph.addPoint(new Point(0.6, 17.1, 0)); // 9
        _graph.addPoint(new Point(0.6, 20.1, 0)); // 10
        _graph.addPoint(new Point(0.0, 27.3, 0)); // 11
        _graph.addPoint(new Point(3.0, 27.3, 0)); // 12
        _graph.addPoint(new Point(0.6, 27.3, 0)); // 13
        _graph.addPoint(new Point(2.1, 0.6, 0)); // 14
        _graph.addPoint(new Point(3.6, 0.6, 0)); // 15
        _graph.addPoint(new Point(5.1, 0.6, 0)); // 16
        _graph.addPoint(new Point(8.1, 0.6, 0)); // 17
        _graph.addPoint(new Point(2.1, 2.1, 0)); // 18
        _graph.addPoint(new Point(2.1, 3.6, 0)); // 19
        _graph.addPoint(new Point(3.6, 3.6, 0)); // 20
        _graph.addPoint(new Point(3.6, 2.1, 0)); // 21
        _graph.addPoint(new Point(5.1, 2.1, 0)); // 22
        _graph.addPoint(new Point(5.1, 3.6, 0)); // 23
        _graph.addPoint(new Point(6.6, 3.6, 0)); // 24
        _graph.addPoint(new Point(6.6, 2.1, 0)); // 25
        _graph.addPoint(new Point(8.1, 2.1, 0)); // 26
        _graph.addPoint(new Point(8.1, 3.6, 0)); // 27
        _graph.addPoint(new Point(9.6, 2.1, 0)); // 28
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
