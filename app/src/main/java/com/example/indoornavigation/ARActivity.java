package com.example.indoornavigation;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.indoornavigation.model.Point;
import com.example.indoornavigation.model.Quat;
import com.example.indoornavigation.model.Route;
import com.example.indoornavigation.model.Vertex;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.Objects;

public class ARActivity extends AppCompatActivity implements SensorEventListener {

    // UI
    TextView guideText;

    // Current location
    Point current = new Point(0,0, 0);
    Vertex vertexCurrent = new Vertex(0,0, 0);
    Vertex viewPoint = new Vertex(0,0, 0);

    // Route
    Route route = new Route(new ArrayList<>());
    int ni;

    // AR variables
    private ArFragment arCam;
    private Node oldNode = null;
    TransformableNode model;
    private int deviceHeight, deviceWidth;
    private int count = 0;
    private Scene.OnUpdateListener sceneUpdate;
    private ArrayList<Double> angleBetweenTwoVectorList = new ArrayList<>();
    int nextPointFrames = 0;
    int afterTrueFrames = 0;
    boolean isARotationPoint = false;
    boolean stopUpdateCurrent = false;
    // Sensor
    double stepLength = 0.5;

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
    float gyroY = 0, gyroX = 0, gyroZ = 0;
    long lastStepTime = 0, lastChangeTime = 0;
    Button stuckBtn;
    boolean forceNextPoint = false;
    boolean stairUp = false;
    boolean stairDown = false;
    boolean stairFinish = false;
    int countStair = 0;
    Button finishStairsBtn;

    public static boolean checkSystemSupport(Activity activity) {

        // checking whether the API version of the running Android >= 24
        // that means Android Nougat 7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String openGlVersion = ((ActivityManager) Objects.requireNonNull(activity.getSystemService(Context.ACTIVITY_SERVICE))).getDeviceConfigurationInfo().getGlEsVersion();

            // checking whether the OpenGL version >= 3.0
            if (Double.parseDouble(openGlVersion) >= 3.0) {
                return true;
            } else {
                Toast.makeText(activity, "App needs OpenGl Version 3.0 or later", Toast.LENGTH_SHORT).show();
                activity.finish();
                return false;
            }
        } else {
            Toast.makeText(activity, "App does not support required Build Version", Toast.LENGTH_SHORT).show();
            activity.finish();
            return false;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        guideText = findViewById(R.id.guideText);
        stuckBtn = findViewById(R.id.stuckBtn);
        stuckBtn.setVisibility(View.GONE);
        finishStairsBtn = findViewById(R.id.finishFloor);
        finishStairsBtn.setVisibility(View.GONE);

        finishStairsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stairFinish = true;
                stairUp = false;
                stairDown = false;
            }
        });

        stuckBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forceNextPoint = true;
            }
        });

        //sensor manager & sensor required to calculate yaw
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        lastStepTime = System.currentTimeMillis();
        lastChangeTime = System.currentTimeMillis();
        registerSensors();

        // Get route
         ArrayList<Point> points = (ArrayList<Point>)getIntent().getSerializableExtra("route");
         route.setPoints(points);

         stepLength = (double) getIntent().getDoubleExtra("stepLength", 0.5);

         current = points.get(0);
//        current = new Point(0,0);
//        // TO debug
//        route.addPoint(new Point(0,0));
//        route.addPoint(new Point(0,2));


        if (route.size()>1){
            ni = 1;
        }else{
            ni = 0;
        }

        updateRouteGuide();

        // Device size
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        deviceHeight = displayMetrics.heightPixels;
        deviceWidth = displayMetrics.widthPixels;


        if (checkSystemSupport(this)) {

            // ArFragment is linked up with its respective id used in the activity_main.xml
            arCam = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arCameraArea);
            if (oldNode==null){
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadRouteModel();
                    }
                },5000);
            }

            sceneUpdate = new Scene.OnUpdateListener() {
                @Override
                public void onUpdate(FrameTime frameTime) {
                    // arCam.onUpdate(frameTime);
                    if(oldNode!=null){
                        updateNode();
                    }
                }
            };
            arCam.getArSceneView().getScene().addOnUpdateListener(sceneUpdate);
        } else {
            return;
        }
    }

    private void updateRouteGuide() {
        if (ni==0){
            // we are at destination already
            return;
        }
        if (stairUp){
            guideText.setText("Go up the stairs");
            return;
        }else if (stairDown){
            guideText.setText("Go down the stairs");
            return;
        }

        int np = ni;
        // current point id
        int cp = ni-1;
        while (true){
            if (np+1 >= route.getPoints().size()){
                // update ui
                double dis = current
                        .distance(route.getPoints().get(route.getPoints().size()-1));
                guideText.setText(dis + " meters to destination.");
                break;
            }
            Point prevPnt = route.getPoints().get(np-1);
            Point currentPnt = route.getPoints().get(np);
            Point nextPnt = route.getPoints().get(np+1);

            if (currentPnt.getZ() - prevPnt.getZ()==1){
                guideText.setText("Go up the stairs");
                break;
            }else if (currentPnt.getZ() - prevPnt.getZ()==-1){
                guideText.setText("Go down the stairs");
                break;
            }

            Point diff0 = currentPnt.sub(prevPnt);
            Point diff1 = nextPnt.sub(currentPnt);
            double cross = diff0.cross(diff1);
            if (cross>0) {
                // left side
                // update ui
                // from cp to np meter
                // if cp-np == 1 then turn to left
                if ((cp-np) == 1){
                    guideText.setText("Turn to left");
                }else{
                    double dis = current
                            .distance(route.getPoints().get(np));
                    guideText.setText(dis + " meters to left.");
                }

                break;
            }else if (cross<0){
                // right side
                // update ui
                // from cp to np meter
                // if cp-np == 1 then turn to right
                if ((cp-np) == 1){
                    guideText.setText("Turn to right");
                }else{
                    double dis = current
                            .distance(route.getPoints().get(np));
                    guideText.setText(dis + " meters to right.");
                }

                break;
            }

            np++;
        }

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

    private void updateNode() {

        if (stairDown || stairUp){
            finishStairsBtn.setVisibility(View.VISIBLE);
        }
        if (stairFinish){
            finishStairsBtn.setVisibility(View.GONE);
        }

        if (oldNode==null) {
            return;
        }

        nextPointFrames++;
        if (isARotationPoint){
            afterTrueFrames++;
        }
        // Rotate model from view point to current location
        Quaternion q = arCam.getArSceneView().getScene().getCamera().getLocalRotation();
        Quat qc = new Quat(q);
        Vector3 normal = qc.normal();
        Vertex normalPoint = new Vertex(normal.x, normal.z, 0).normalize();
        float initial2dRotate = (float)Math.atan2(normalPoint.getY(), normalPoint.getX());


        if (current!=null){
            vertexCurrent.setX(current.getX());
            vertexCurrent.setY(current.getY());
            vertexCurrent.setZ(current.getZ());


            // TODO: 1.8 m or 1 m?
            // View point
            //  O    |         \
            // /|\  1.5m       1.8m
            // / \   | __ 1m __  \
            // TODO: use pitch
            double d = 2;
            if (pitch != 90) {
                // height assumed to 1.65
                double tanPitch = 0.0;
                if (Math.tan(Math.toRadians(Math.abs(pitch)))==0){
                    tanPitch = 0.1;
                }else{
                    tanPitch = (1/Math.tan(Math.toRadians(Math.abs(pitch))));
                }
                if (tanPitch>0.3){
                    tanPitch = 0.3;
                }
                d = 1.5 * tanPitch;
            }
            viewPoint = vertexCurrent.add(new Vertex(d*Math.sin(Math.toRadians(yaw-originYaw)),
                    d*Math.cos(Math.toRadians(yaw-originYaw)),
                    0));


            Vertex nextPnt = new Vertex(route.getPoints().get(ni).getX(),
                    route.getPoints().get(ni).getY(),
                    route.getPoints().get(ni).getZ());
            Vertex prevPnt;
            if (ni>0){
                prevPnt = new Vertex(route.getPoints().get(ni-1).getX(),
                        route.getPoints().get(ni-1).getY(),
                        route.getPoints().get(ni-1).getZ());
            }else{
                prevPnt = new Vertex(route.getPoints().get(ni).getX(),
                        route.getPoints().get(ni).getY(),
                        route.getPoints().get(ni).getZ());
            }

            // Rotate from view to destination
            // Direction from view to current == model initial direction
            final Vertex diffFromViewToCurrent = vertexCurrent.sub(viewPoint);
            final Vertex directionFromViewToCurrent = diffFromViewToCurrent.normalize();
            double alpha = Math.atan2(directionFromViewToCurrent.getY(), directionFromViewToCurrent.getX());

            // Direction from view to next point on route
            final Vertex diffFromViewToNext = nextPnt.sub(viewPoint);
            final Vertex diffFromCurrentToNext = nextPnt.sub(vertexCurrent);
            final Vertex diffFromNextToPrev = prevPnt.sub(nextPnt);

            // Distance from view to next point
            loadRouteModel();

            final Vertex directionFromViewToNext = diffFromViewToNext.normalize();
            double beta = Math.atan2(directionFromViewToNext.getY(), directionFromViewToNext.getX());

            double rotationDegree;
            double angleBetweenTwoVector = Math.acos(
                    directionFromViewToNext.dot(directionFromViewToCurrent)/
                            (directionFromViewToNext.length()*directionFromViewToCurrent.length())
            );


            // TODO: comment this
//            angleBetweenTwoVector = modifyAngle(angleBetweenTwoVector);
            // TODO: 140?
            if (Math.toDegrees(angleBetweenTwoVector)>150 && !stairUp && !stairDown){
                if (isARotationPoint  && afterTrueFrames>3){
                    modifyCurrent(prevPnt);
                    stopUpdateCurrent = false;
                    isARotationPoint = false;
//                    nextPointFrames = 0; //?
                    afterTrueFrames = 0;
//                    test.setText("is rotation = " + isARotationPoint + "\n" +
//                            "current : " + current.getX() + ", "+ current.getY());
                }
            }

            if (stairUp){
                rotationDegree = Math.PI;
            }else if (stairDown){
                rotationDegree = 0.0;
            }else if (Math.toDegrees(angleBetweenTwoVector)>130){

                rotationDegree = Math.PI;
            }else{
                rotationDegree = beta - alpha;
            }

            final Quaternion finalQ;
            final Quaternion faceToBed;
            final Quaternion lookFromViewToNext;
            if (Math.toDegrees(angleBetweenTwoVector)>25 &&  Math.toDegrees(angleBetweenTwoVector)<145){
                if (nextPointFrames<20){
                    isARotationPoint = true;
                    stopUpdateCurrent = true;
//                    test.setText("is rotation = " + isARotationPoint + "\n" +
//                            "current : " + current.getX() + ", "+ current.getY());
                }
            }
            if (stairUp || stairDown){

                faceToBed = Quaternion.axisAngle(Vector3.right(), 90f);
                lookFromViewToNext = Quaternion.axisAngle(Vector3.up(), (float)Math.toDegrees(initial2dRotate+rotationDegree)+270f);

                Quaternion finalQTmp = Quaternion.multiply(lookFromViewToNext, faceToBed );

                Quaternion faceToBedTmp = new Quaternion();
                if (stairUp){
                    faceToBedTmp = Quaternion.axisAngle(Vector3.up(), 90f);
                }else{
                    faceToBedTmp = Quaternion.axisAngle(Vector3.down(), 90f);
                }
                finalQ = Quaternion.multiply(finalQTmp, faceToBedTmp );


                // TODO: 45 and 135?
            } else if (Math.toDegrees(angleBetweenTwoVector)>35 &&  Math.toDegrees(angleBetweenTwoVector)<125){

                finalQ = Quaternion.axisAngle(Vector3.up(), (float)Math.toDegrees(initial2dRotate+rotationDegree)+270f);
            }else{

                faceToBed = Quaternion.axisAngle(Vector3.right(), 90f);
                lookFromViewToNext = Quaternion.axisAngle(Vector3.up(), (float)Math.toDegrees(initial2dRotate+rotationDegree)+270f);

                finalQ = Quaternion.multiply(lookFromViewToNext, faceToBed );
            }

            model.setWorldRotation(finalQ);


            if (ni!=0){
                // to debug
//                    guideText.setText( "stop = " + stopUpdateCurrent  + " - yaw = "+ yaw +
//                                    "Diff yaw = " + (yaw-originYaw) + "\n"+
//                                    "current = " + current.getX()+", "+ current.getY()+ ", "+ current.getZ() +"\n"+
//                                    "view = " +viewPoint.getX()+", "+viewPoint.getY()+  ", "+ viewPoint.getZ()+ "\n"+
//                              "next point" + nextPnt.getX() + ", " + nextPnt.getY()+ ", "+ nextPnt.getZ() + "\n"+
//                                    "distance to next point = "+ diffFromViewToNext.length() + "\n" +
//                            "distance from current to next = " + diffFromCurrentToNext.length()+ "\n"+
//                            "is rotation :" + isARotationPoint + "\n" +
//                            "pitch: " + pitch + "\n"+
//                            "stuck = " + forceNextPoint+ "\n"+
//                            "angleBetweenTwoVector => "+ Math.toDegrees(angleBetweenTwoVector)+"\n"+
//                            "ni => "+ ni+"\n"+
//                            "rotationDegree => "+ rotationDegree+"\n" +
//                            "stair => "+ stairUp + " - " + stairDown + "\n" +
//                            "count stairs => "+ countStair+"\n" +
//                            "directionFromViewToNext => " + directionFromViewToNext.getX() + ", " + directionFromViewToNext.getY() + ", "+ directionFromViewToNext.getZ()+"\n"+
//                                    "directionFromViewToCurrent => " + directionFromViewToCurrent.getX() + ", " + directionFromViewToCurrent.getY() + ", "+ directionFromViewToCurrent.getZ()
//
//                            );

//                long currentMillis = System.currentTimeMillis();
//                test.setText("diff step time: " + Math.abs(currentMillis - lastStepTime)+"\n"+
//                        "diff changes time: " + Math.abs(currentMillis - lastChangeTime));
                int timeThresh = 5000;
                if (ni<=3){
                    timeThresh = 10000;
                }
                if (Math.abs(lastStepTime-System.currentTimeMillis())>timeThresh &&
                        Math.abs(lastChangeTime-System.currentTimeMillis())>timeThresh) {
                    stuckBtn.setVisibility(View.VISIBLE);
                }else{
                    stuckBtn.setVisibility(View.GONE);
                }
                if (forceNextPoint){
                    // go next point
                    lastChangeTime = System.currentTimeMillis();
                    forceNextPoint = false;
                    stuckBtn.setVisibility(View.GONE);
                    if (!route.finish(ni)){
                        ni = route.next(ni);
                        // check stairs
                        Vertex nextPntC = new Vertex(route.getPoints().get(ni).getX(),
                                route.getPoints().get(ni).getY(),
                                route.getPoints().get(ni).getZ());
                        Vertex prevPntC = new Vertex(0,0,0);
                        if (ni>0){
                            prevPntC = new Vertex(route.getPoints().get(ni-1).getX(),
                                    route.getPoints().get(ni-1).getY(),
                                    route.getPoints().get(ni-1).getZ());
                        }else{
                            prevPntC = new Vertex(route.getPoints().get(ni).getX(),
                                    route.getPoints().get(ni).getY(),
                                    route.getPoints().get(ni).getZ());
                        }
                        if (nextPntC.getZ()-prevPntC.getZ()== 1){
                            stairUp =true;
                        }else if (nextPntC.getZ()-prevPntC.getZ()== -1){
                            stairDown = true;
                        }
                        stairFinish = false;
                        updateRouteGuide();
                        angleBetweenTwoVectorList = new ArrayList<>();
                        nextPointFrames = 0;
                    }else{
                        // View point is on destination, put marker
                        loadDestinationModel();
                    }
                    modifyCurrent(prevPnt);
                }

                if (!route.finish(ni)){
                    // TODO:1.?
                    // TODO: current or view
                    if (diffFromCurrentToNext.length()<0.5 || stairFinish){
                        if (stairFinish){
                            modifyCurrentStair(prevPnt, nextPnt);
                        }
                        ni = route.next(ni);
                        // check stairs
                        Vertex nextPntC = new Vertex(route.getPoints().get(ni).getX(),
                                route.getPoints().get(ni).getY(),
                                route.getPoints().get(ni).getZ());
                        Vertex prevPntC = new Vertex(0,0,0);
                        if (ni>0){
                            prevPntC = new Vertex(route.getPoints().get(ni-1).getX(),
                                    route.getPoints().get(ni-1).getY(),
                                    route.getPoints().get(ni-1).getZ());
                        }else{
                            prevPntC = new Vertex(route.getPoints().get(ni).getX(),
                                    route.getPoints().get(ni).getY(),
                                    route.getPoints().get(ni).getZ());
                        }
                        if (nextPntC.getZ()-prevPntC.getZ()== 1){
                            stairUp =true;
                        }else if (nextPntC.getZ()-prevPntC.getZ()== -1){
                            stairDown = true;
                        }
                        stairFinish = false;

                        updateRouteGuide();
                        angleBetweenTwoVectorList = new ArrayList<>();
                        nextPointFrames = 0;
                    }
                }else{
                    // View point is on destination, put marker
                    if (diffFromCurrentToNext.length()  <0.5){
                         loadDestinationModel();
                    }
                }
            }else{
                // Route has just one point so
                // View point is on destination, put marker
                if (diffFromCurrentToNext.length()<0.5){
                     loadDestinationModel();
                }
            }
        }

    }

    private void modifyCurrentStair(Vertex prevPnt, Vertex nextPnt) {
        current.setX(prevPnt.getX());
        current.setY(prevPnt.getY());
        current.setZ(nextPnt.getZ());

    }

    private void modifyCurrent(Vertex prevPnt) {
        current.setY(prevPnt.getY());
        current.setX(prevPnt.getX());
        current.setZ(prevPnt.getZ());
//        // left
//        if (yaw>230 && yaw <300){
//            current.setY(prevPnt.getY());
//            current.setX(prevPnt.getX());
//
//            // down
//        }else if (yaw>130 && yaw< 200){
//            current.setX(prevPnt.getX());
//            current.setY(prevPnt.getY());
//
//            // right
//        }else if (yaw>30 && yaw<100){
//            current.setY(prevPnt.getY());
//            current.setX(prevPnt.getX());
//
//            // up
//        }else if ((yaw>300 && yaw <360) || (yaw>0 && yaw<30)){
//            current.setX(prevPnt.getX());
//            current.setY(prevPnt.getY());
//
//        }
    }

    private double modifyAngle(double angleBetweenTwoVector) {
        double meanAngle = 0.;
        if (angleBetweenTwoVectorList.size()>0){
            if (angleBetweenTwoVectorList.size()>5){
                angleBetweenTwoVectorList.remove(0);
            }
            angleBetweenTwoVectorList.add(angleBetweenTwoVector);
            for (int i = 0; i < angleBetweenTwoVectorList.size() ; i++) {
                meanAngle +=  angleBetweenTwoVectorList.get(i);
            }

            meanAngle /= angleBetweenTwoVectorList.size();
        }else{
            angleBetweenTwoVectorList.add(angleBetweenTwoVector);
            meanAngle = angleBetweenTwoVector;
        }

        return meanAngle;
    }

    private void loadDestinationModel() {
        ModelRenderable.builder()
                .setSource(ARActivity.this, Uri.parse("arrow_location.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(modelRenderable ->{
                    arCam.getArSceneView().getScene().removeOnUpdateListener(sceneUpdate);
                    addDestinationNode(modelRenderable);
                })
                .exceptionally(throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ARActivity.this);
                    builder.setMessage("Something is not right" + throwable.getMessage()).show();
                    return null;
                });
    }


    private void addDestinationNode(ModelRenderable modelRenderable) {
        arCam.getArSceneView().getPlaneRenderer().setVisible(false);

        Node node = new Node();

        // Remove old object
        if(oldNode!=null){
            Node nodeToRemove = arCam.getArSceneView().getScene().getChildren().get(1);
            arCam.getArSceneView().getScene().removeChild(nodeToRemove);

        }
        node.setParent(arCam.getArSceneView().getScene());
        Camera camera = arCam.getArSceneView().getScene().getCamera();
        Ray ray = camera.screenPointToRay(deviceWidth/2, 2*deviceHeight/3);

        model = new TransformableNode(arCam.getTransformationSystem());
        model.getScaleController().setMaxScale(0.25f);
        model.getScaleController().setMinScale(0.2f);
        model.setLocalPosition(ray.getPoint(5f));
        model.setParent(node);
        model.setRenderable(modelRenderable);
        model.getTransformationSystem().selectNode(null);
        oldNode = node;
        arCam.getArSceneView().getScene().addChild(oldNode);

    }

    private void loadRouteModel() {
        ModelRenderable.builder()
                .setSource(ARActivity.this, Uri.parse("red_arrow_chevrons_wayfinding.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(modelRenderable -> addRouteNode(modelRenderable))
                .exceptionally(throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ARActivity.this);
                    builder.setMessage("Something is not right" + throwable.getMessage()).show();
                    return null;
                });
    }

    private void addRouteNode(ModelRenderable modelRenderable ) {
        arCam.getArSceneView().getPlaneRenderer().setVisible(false);

        Node node = new Node();

        // Remove old object
        if(oldNode!=null){
            if (arCam.getArSceneView().getScene().getChildren().size()>1){
                Node nodeToRemove = arCam.getArSceneView().getScene().getChildren().get(1);
                arCam.getArSceneView().getScene().removeChild(nodeToRemove);
            }
        }
        node.setParent(arCam.getArSceneView().getScene());
        Camera camera = arCam.getArSceneView().getScene().getCamera();
        // TODO: use pitch to determine the x and y here
        float sh = ((Math.abs(pitch)+90)/180) + 1; // max = 2, min = 1.5
        Ray ray = camera.screenPointToRay(deviceWidth/2, sh*deviceHeight/3);

        model = new TransformableNode(arCam.getTransformationSystem());
        //TODO: scale?
        // TODO: use pitch to determine scale
        double scale = (Math.abs(pitch)+90)/180 + (1.); // max = 0.5, min = 1
        // TODO:
        model.getScaleController().setMaxScale((float)scale*6/1000); // TODO: 6/1000
        model.getScaleController().setMinScale((float)scale*4/1000); // TODO: 4/1000
        // TODO: use pitch to determine distance
        double rayDis = Math.cos(Math.toRadians(Math.abs(pitch)))*10;
        model.setLocalPosition(ray.getPoint((float)rayDis));
        model.setParent(node);
        model.setRenderable(modelRenderable);
        model.getTransformationSystem().selectNode(null);
        oldNode = node;
        arCam.getArSceneView().getScene().addChild(oldNode);

    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        if (firstCheck<20){
            firstCheck++;
            return;
        }

        gyroscope(event);
        getSensorData(event);
        rotation();
        // TODO: check gyroX and gyroZ and check threshold also see the changes in stairs
        // I want to detect step in stairs even gyros are greater than 0.3
        // for now we disable stepDetector in stairs
        if ((Math.abs(gyroY)<0.3 && Math.abs(gyroZ)<0.3 && Math.abs(gyroX)<0.3 ) && !stairUp && !stairDown){
            stepDetector(event);
        }else if (Math.abs(gyroY)>=0.3 || Math.abs(gyroZ)>=0.3 || Math.abs(gyroX)>=0.3 ){
            lastChangeTime = System.currentTimeMillis();
        }

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


    private void stepDetector(SensorEvent event) {
        // Step
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){

            // Calculate magnitude of combined sensor data
            double magnitude = Math.sqrt(
                    accelerometerData[0] * accelerometerData[0] +
                            accelerometerData[1] * accelerometerData[1] +
                            accelerometerData[2] * accelerometerData[2]
            );


//            test.setText( "Yaw = " + yaw+"\n"+
//                    "mag = " + magnitude +"\n"+
//                    "steps = " + stepCount + "\n"+
//                    "cx = "+  current.getX() + "\n"+
//                    "cy = " + current.getY());

            // Detect peaks
            if (magnitude > STEP_THRESHOLD && !isPeak) {
                // Detected a peak (potential step)
                isPeak = true;
            } else if (magnitude < MIN_STEP_THRESHOLD && isPeak) {
                // Step has ended
                isPeak = false;
                // Increment step count here
                stepCount++;
                // update current
                long currentMillis = System.currentTimeMillis();
//                test.setText("current: "+ currentMillis+"\n"+
//                        "last step: " + lastStepTime+ "\n"+
//                        "diff step time: " + Math.abs(currentMillis - lastStepTime)+"\n"+
//                        "diff changes time: " + Math.abs(currentMillis - lastChangeTime));
                if (Math.abs(currentMillis - lastStepTime)>200){
                    if(!stairDown && !stairUp){
                        updateCurrent();
                    }else if (stairUp){
                        countStair+=1;

                        if (countStair>20){
                            stairFinish = true;
                            stairUp = false;
//                            current.setZ(route.getPoints().get(ni).getZ());
                            countStair = 0;
                        }
                    }else if (stairDown){
                        countStair+=1;

                        if (countStair>20){
                            stairFinish = true;
                            stairDown = false;
//                            current.setZ(route.getPoints().get(ni).getZ());
                            countStair = 0;
                        }
                    }
                    lastStepTime = currentMillis;
                    lastChangeTime = currentMillis;
                }

            }
        }
    }


    private void updateCurrent() {
        if (stopUpdateCurrent){
            return;
        }
        updateRouteGuide();
        // left
        if (yaw>240 && yaw <290){
            current.x -= stepLength;

            // down
        }else if (yaw>140 && yaw< 190){
            current.y -= stepLength;

            // right
        }else if (yaw>40 && yaw<90){
            current.x += stepLength;

            // up
        }else if ((yaw>320 && yaw <360) || (yaw > 0 && yaw <30 ) ){
            current.y += stepLength;

        }else{
            current.x += stepLength * Math.sin(Math.toRadians(yaw-originYaw));
            current.y += stepLength * Math.cos(Math.toRadians(yaw-originYaw));
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
            gyroX = Math.abs(event.values[0]);
            gyroY = event.values[1];
            gyroZ = Math.abs(event.values[2]);
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
            if (ignoreCnt<=5 && Math.abs(yaw - value )>10 && Math.abs(gyroY)<0.1) { //TODO
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
            if (ignoreCnt<=5 && Math.abs(yaw - value )>10 && Math.abs(gyroY)<0.1) { //TODO
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
        if (!registered){
            registerSensors();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}