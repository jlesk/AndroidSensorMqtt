package com.example.sensorapplication;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.bson.BSONObject;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

//import com.example.sensorapplication.R;
import com.mongodb.BasicDBObject;
//import com.mongodb.DB;
import com.mongodb.DBCollection;
//import com.mongodb.DBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
//import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private LinearLayout sensorLayout;
    private TextView textView;
    private int sensorInd;

    private  SensorManager sensorManager;
    private Sensor lightSensor;
    private Sensor proximitySensor;
    private Sensor rotationSensor;
    private Sensor gamerotationSensor;
    private Sensor georotationSensor;
    private Sensor stepCounterSensor;
    private Sensor accelerometerSensor;
    private Sensor linearaccelerationSensor;
    private Sensor magneticfieldSensor;
    private Sensor gyroscopeSensor;
    private SensorData sensorData;

    private Vibrator v;
    MqttHelper mqttHelper;
    TextView dataReceived;
    //DB db;

    long startTime = 0;

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            MqttMessage message = new MqttMessage();
            Log.w("Debug", "3");

            message.setPayload(sensorData.toJSon().getBytes());
            Log.w("Debug", "4");
            message.setRetained(false);
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            timerHandler.postDelayed(this, 500);
            Log.w("timer", "timer");
            StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
            //StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old)
            //            .permitDiskWrites()
            //            .build());
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            try {
                MongoClientURI mongoUri = new MongoClientURI("mongodb://192.168.0.10:27017");
                Log.w("timer", "got MongoClientURI");
                MongoClient mongoClient = new MongoClient(mongoUri);
                Log.w("timer", "got mongoClient");
                MongoDatabase db = mongoClient.getDatabase("TEST");
                Log.w("timer", "got database");


                // Fetching the collection from the mongodb.
                MongoCollection<Document> coll = db.getCollection("SENSORS_ANDROID");
                Log.w("timer", "got collection");
                Document doc = new Document();
                Log.w("timer", "document created");
                doc.put("sensordata", sensorData.toDoc());

                Log.w("timer", "document put");
                coll.insertOne(doc);
                Log.w("timer", "document inserted");

            } catch (Exception e) {
                Log.w("timer", "Error connecting to mongodb database " );
            }
            StrictMode.setThreadPolicy(old);

            try {
                mqttHelper.mqttAndroidClient.publish("sensors", message);
                Log.w("timer", "mqtt published");


            } catch (org.eclipse.paho.client.mqttv3.MqttPersistenceException mqttPersistenceException) {
                Log.e("timer", "mqttPersistenceException");
            } catch (org.eclipse.paho.client.mqttv3.MqttException mqttException) {
                Log.e("timer", "mqttException");
            }
            sensorData.ClearData();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorData = new SensorData();
        //setContentView(R.layout.activity_sensor);
        setContentView(R.layout.activity_main);
        sensorLayout = findViewById(R.id.sensors_layout);
        textView = findViewById(R.id.sensors);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        gamerotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        georotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        linearaccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        magneticfieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        mqttHelper = new MqttHelper(getApplicationContext());
        dataReceived = (TextView) findViewById(R.id.dataReceived);

        startMqtt();

        timerHandler.postDelayed(timerRunnable, 500);
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                Log.w("location", "latitude " + location.getLatitude() + ", longitude " + location.getLongitude());
                sensorData.SetLocation(location.getLatitude(), location.getLongitude());
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

// Register the listener with the Location Manager to receive location updates
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        } catch (Exception ex)  {
            Log.e("location", "Error creating location service: " + ex.getMessage() );
        }

    }



    public void sensorsList(View view){
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);

        String sensorInfo = "";
        for (Sensor s : sensorList){
            sensorInfo= sensorInfo + s.getName()+ "\n";
        }
        textView.setText(sensorInfo);
    }
    public boolean checkSensorAvailability(int sensorType){
        boolean isSensor = false;
        if(sensorManager.getDefaultSensor(sensorType) != null){
            isSensor = true;
        }
        Log.d("checkSensorAvailability" ,""+isSensor);
        return  isSensor;
    }
    public void lightSensor(View view){
        if(checkSensorAvailability(Sensor.TYPE_LIGHT)){
            sensorInd = Sensor.TYPE_LIGHT;
        }
    }
    public void proximitySensor(View view){
        if(checkSensorAvailability(Sensor.TYPE_PROXIMITY)){
            sensorInd = Sensor.TYPE_PROXIMITY;
        }
    }
    public void rotationSensor(View view){
        if(checkSensorAvailability(Sensor.TYPE_GAME_ROTATION_VECTOR)){
            sensorInd = Sensor.TYPE_GAME_ROTATION_VECTOR;
        }
    }
    public void stepCounterSensor(View view){
        if(checkSensorAvailability(Sensor.TYPE_GAME_ROTATION_VECTOR)){
            sensorInd = Sensor.TYPE_STEP_DETECTOR;
        }
    }
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        //check sensor type matches current sensor type set by button click
        Log.w("Mqtt", "sensor changed " + event.sensor.getType() +  " " + sensorInd);
        //if( event.sensor.getType() == sensorInd){
            Log.w("Mqtt", "sensorInd");
            //light sensor
            if(event.sensor.getType() ==  Sensor.TYPE_LIGHT){
                float valueZ = event.values[0];
                Toast.makeText(this, "luminescence "+valueZ,Toast.LENGTH_LONG).show();
                Log.w("Mqtt", "valueZ " + valueZ);

            }else if(event.sensor.getType() ==  Sensor.TYPE_PROXIMITY){
                //proximity sensor
                float distance = event.values[0];
                Toast.makeText(this, "proximity "+distance,Toast.LENGTH_LONG).show();
                Log.w("Mqtt", "distance " + distance);

            }else if(event.sensor.getType() ==  Sensor.TYPE_STEP_DETECTOR){
                //step counter
                float steps = event.values[0];
                textView.setText("steps : "+steps);
                Log.w("Mqtt", "steps " + steps);

            }else if(event.sensor.getType() ==  Sensor.TYPE_GAME_ROTATION_VECTOR){
                //rotation sensor
                float[] rotMatrix = new float[9];
                float[] rotVals = new float[3];

                SensorManager.getRotationMatrixFromVector(rotMatrix, event.values);
                SensorManager.remapCoordinateSystem(rotMatrix,
                        SensorManager.AXIS_X, SensorManager.AXIS_Y, rotMatrix);

                SensorManager.getOrientation(rotMatrix, rotVals);
                float azimuth = (float) Math.toDegrees(rotVals[0]);
                float pitch = (float) Math.toDegrees(rotVals[1]);
                float roll = (float) Math.toDegrees(rotVals[2]);
                Log.w("Mqtt", "azimuth " + azimuth);
                Log.w("Mqtt", "pitch " + pitch);
                Log.w("Mqtt", "roll " + roll);
            }
            else if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                Log.w("Mqtt", "TYPE_ROTATION_VECTOR");
                float[] rotMatrix = new float[9];
                float[] rotVals = new float[3];

                SensorManager.getRotationMatrixFromVector(rotMatrix, event.values);
                SensorManager.remapCoordinateSystem(rotMatrix,
                        SensorManager.AXIS_X, SensorManager.AXIS_Y, rotMatrix);

                SensorManager.getOrientation(rotMatrix, rotVals);
                float azimuth = (float) Math.toDegrees(rotVals[0]);
                float pitch = (float) Math.toDegrees(rotVals[1]);
                float roll = (float) Math.toDegrees(rotVals[2]);
                Log.w("Mqtt", "azimuth " + azimuth);
                Log.w("Mqtt", "pitch " + pitch);
                Log.w("Mqtt", "roll " + roll);
                sensorData.SetRotationVector(azimuth, pitch, roll);

            }
            else if(event.sensor.getType() == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
                Log.w("Mqtt", "TYPE_GEOMAGNETIC_ROTATION_VECTOR");
                float[] rotMatrix = new float[9];
                float[] rotVals = new float[3];

                SensorManager.getRotationMatrixFromVector(rotMatrix, event.values);
                SensorManager.remapCoordinateSystem(rotMatrix,
                        SensorManager.AXIS_X, SensorManager.AXIS_Y, rotMatrix);

                SensorManager.getOrientation(rotMatrix, rotVals);
                float azimuth = (float) Math.toDegrees(rotVals[0]);
                float pitch = (float) Math.toDegrees(rotVals[1]);
                float roll = (float) Math.toDegrees(rotVals[2]);
                Log.w("Mqtt", "azimuth " + azimuth);
                Log.w("Mqtt", "pitch " + pitch);
                Log.w("Mqtt", "roll " + roll);

            }
            else if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                Log.w("Mqtt", "TYPE_ACCELEROMETER");
                float Xpp = event.values[0];
                float Ypp = event.values[1];
                float Zpp = event.values[2];
                Log.w("Mqtt", "Xpp " + Xpp);
                Log.w("Mqtt", "Ypp " + Ypp);
                Log.w("Mqtt", "Zpp " + Zpp);
                sensorData.SetAcceleration(Xpp, Ypp, Zpp);

            }
            else if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                Log.w("Mqtt", "TYPE_LINEAR_ACCELERATION");
                float Xpp = event.values[0];
                float Ypp = event.values[1];
                float Zpp = event.values[2];
                Log.w("Mqtt", "Xpp " + Xpp);
                Log.w("Mqtt", "Ypp " + Ypp);
                Log.w("Mqtt", "Zpp " + Zpp);
            }
            else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                Log.w("Mqtt", "TYPE_MAGNETIC_FIELD");
                float Xpp = event.values[0];
                float Ypp = event.values[1];
                float Zpp = event.values[2];
                Log.w("Mqtt", "Xpp " + Xpp);
                Log.w("Mqtt", "Ypp " + Ypp);
                Log.w("Mqtt", "Zpp " + Zpp);
            }
            else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                Log.w("Mqtt", "TYPE_GYROSCOPE");
                float X = event.values[0];
                float Y = event.values[1];
                float Z = event.values[2];
                Log.w("Mqtt", "X rad/s " + X);
                Log.w("Mqtt", "Y rad/s" + Y);
                Log.w("Mqtt", "Z rad/s" + Z);
            }
            else if(event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                Log.w("Mqtt", "TYPE_STEP_DETECTOR");
                float steps = event.values[0];
                textView.setText("steps : "+steps);
                Log.w("Mqtt", "steps " + steps);
            }

        //}
    }


    private void startMqtt(){
        mqttHelper.mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.w("Debug","Connected");
            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("Debug",mqttMessage.toString());
                /*Log.w("Debug","test");

                MqttMessage message = new MqttMessage();
                Log.w("Debug", "3");

                message.setPayload("hello".getBytes("UTF-8"));
                Log.w("Debug", "4");
                message.setRetained(false);
                Log.w("Debug", "5");
                mqttHelper.mqttAndroidClient.publish("demo2", message);
                Log.w("Debug", "6");
                mqttHelper.mqttAndroidClient.publish("demo2", mqttMessage);
                Log.w("Debug", "7");*/
                dataReceived.setText(mqttMessage.toString());
                v.vibrate(300);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, lightSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, proximitySensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, rotationSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, stepCounterSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}