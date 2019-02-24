package com.example.sensorapplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Vector;

public class SensorData {
    private Vector<Float> RotationVectorAzimuth;
    private Vector<Float> RotationVectorPitch;
    private Vector<Float> RotationVectorRoll;
    public SensorData() {
        RotationVectorAzimuth = new Vector<>();
        RotationVectorPitch = new Vector<>();
        RotationVectorRoll = new Vector<>();
    }
    public String toJSon() {
        try {
            // Here we convert Java Object to JSON
            JSONObject jsonObj = new JSONObject();

            JSONObject jsonAdd = new JSONObject(); // we need another object to store the address
            jsonAdd.put("Azimuth", GetMean(RotationVectorAzimuth));
            jsonAdd.put("Pitch", GetMean(RotationVectorPitch));
            jsonAdd.put("Roll", GetMean(RotationVectorRoll));

            // We add the object to the main object
            jsonObj.put("RotationVector", jsonAdd);

            return jsonObj.toString();

        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        return "";
    }

    public void SetRotationVector(float azimuth, float pitch, float roll) {
        RotationVectorAzimuth.add(azimuth);
        RotationVectorPitch.add(pitch);
        RotationVectorRoll.add(roll);
    }

    public float GetMean(Vector<Float> v) {
        float sum = 0;
        int len = v.size();

        for (int i = 0; i < len; i++) {
            sum += v.get(i);
        }

        v.clear();
        return  sum / len;
    }
}
