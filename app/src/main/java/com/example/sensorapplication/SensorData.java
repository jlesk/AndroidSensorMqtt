package com.example.sensorapplication;

import android.util.Log;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Vector;

public class SensorData {
    private Vector<Float> RotationVectorAzimuth;
    private Vector<Float> RotationVectorPitch;
    private Vector<Float> RotationVectorRoll;
    private Vector<Double> Latitude;
    private Vector<Double> Longitude;
    private Vector<Float> Xpp;
    private Vector<Float> Ypp;
    private Vector<Float> Zpp;


    public SensorData() {
        RotationVectorAzimuth = new Vector<>();
        RotationVectorPitch = new Vector<>();
        RotationVectorRoll = new Vector<>();
        Latitude = new Vector<>();
        Longitude = new Vector<>();
        Xpp = new Vector<>();
        Ypp = new Vector<>();
        Zpp = new Vector<>();
    }

    public String toJSon() {
        try {
            // Here we convert Java Object to JSON
            JSONObject jsonObj = new JSONObject();

            JSONObject rotation = new JSONObject(); // we need another object to store the address
            rotation.put("Azimuth", GetMeanFloat(RotationVectorAzimuth));
            rotation.put("Pitch", GetMeanFloat(RotationVectorPitch));
            Log.w("toJSon", "roll " + GetMeanFloat(RotationVectorRoll));

            rotation.put("Roll", GetMeanFloat(RotationVectorRoll));

            // We add the object to the main object
            jsonObj.put("RotationVector", rotation);

            //JSONObject location = new JSONObject(); // we need another object to store the address
            //location.put("Latitude", GetMeanDouble(Latitude));
            //location.put("Longitude", GetMeanDouble(Longitude));

            //jsonObj.put("Location", location);

            //JSONObject acceleration = new JSONObject(); // we need another object to store the address
            //rotation.put("Xpp", GetMeanFloat(Xpp));
            //rotation.put("Ypp", GetMeanFloat(Ypp));
            //rotation.put("Zpp", GetMeanFloat(Zpp));

            // We add the object to the main object
            //jsonObj.put("Acceleration", acceleration);

            return jsonObj.toString();

        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        return "";
    }
    public Document toDoc() {
        try {
            // Here we convert Java Object to JSON
            Document pdoc = new Document();

            Document doc = new Document();
            Log.w("timer", "document created");
            doc.put("Azimuth", GetMeanFloat(RotationVectorAzimuth));
            Log.w("toJSon", "roll " + GetMeanFloat(RotationVectorRoll));

            doc.put("Pitch", GetMeanFloat(RotationVectorPitch));
            doc.put("Roll", GetMeanFloat(RotationVectorRoll));
            pdoc.put("Rotation", doc);

            return pdoc;

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return new Document("","");
    }
    public void SetRotationVector(float azimuth, float pitch, float roll) {
        Log.w("SetRotationVector", "azimuth " + azimuth);
        Log.w("SetRotationVector", "pitch " + pitch);
        Log.w("SetRotationVector", "roll " + roll);

        RotationVectorAzimuth.add(azimuth);
        RotationVectorPitch.add(pitch);
        RotationVectorRoll.add(roll);
    }

    public void SetLocation(double latitude, double longitude) {
        Latitude.add(latitude);
        Longitude.add(longitude);
    }

    public void SetAcceleration(float xpp, float ypp, float zpp) {
        Xpp.add(xpp);
        Ypp.add(ypp);
        Zpp.add(zpp);
    }

    public void ClearData() {
        RotationVectorAzimuth.clear();
        RotationVectorPitch.clear();
        RotationVectorRoll.clear();
        Latitude.clear();
        Longitude.clear();
        Xpp.clear();
        Ypp.clear();
        Zpp.clear();
    }
    public double GetMeanDouble(Vector<Double> v) {
        double sum = 0;
        int len = v.size();

        for (int i = 0; i < len; i++) {
            sum += v.get(i);
        }

        return  sum / len;
    }


    public float GetMeanFloat(Vector<Float> v) {
        float sum = 0;
        int len = v.size();

        for (int i = 0; i < len; i++) {
            sum += v.get(i);
        }

        return  sum / len;
    }
}
