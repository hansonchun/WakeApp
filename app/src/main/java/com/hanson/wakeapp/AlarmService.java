package com.hanson.wakeapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import android.location.LocationListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by Hanson on 2017-04-21.
 */

public class AlarmService extends Service implements GeocodeAPI.AsyncResponse {

    private static final String GEOCODE_API_BASE = "https://maps.googleapis.com/maps/api/geocode";
    private static final String OUT_JSON = "/json";
    private static final String API_KEY = "AIzaSyB0tUr4a3hdW6lTaTi61hRqpQlf8DMoFkc";

    public static final String ALARM_RESULT = "com.hanson.wakeapp.AlarmService.REQUEST_PROCESSED";

    public LocationManager locationManager;
    public LocationListener locationListener;
    public double currentLat;
    public double currentLon;
    public double destLat;
    public double destLong;

    DatabaseHelper mDatabaseHelper;
    LocalBroadcastManager broadcaster;

    Alarm mAlarm;
    Place selectedPlace;

    float threshold;

    Context context;

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    @Override
    public void onCreate() {
        context = this;
        mDatabaseHelper = new DatabaseHelper(this);
        broadcaster = LocalBroadcastManager.getInstance(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();

        mAlarm = new Alarm();
        selectedPlace = mAlarm.mPlace;

        String thresholdString = prefs.getString("thresholdPref", "");
        threshold = Float.valueOf(thresholdString);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Reset isRunning variable
        editor.putBoolean("isRunning", false);
        editor.apply();

        // Get destination lat and long
        int currTransfer = selectedPlace.getTransfer();
        getLatLong(selectedPlace.addresses.get(currTransfer));

        // Start listening for location updates
        startListener();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        locationManager.removeUpdates(locationListener);

        super.onDestroy();
    }

    private void startListener() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                currentLat = location.getLatitude();
                currentLon = location.getLongitude();

                checkForArrival();
                //Toast.makeText(getBaseContext(), currentLat + "-" + currentLon, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }
            @Override
            public void onProviderEnabled(String provider) {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if(lastKnownLocation != null) {
                    currentLat = lastKnownLocation.getLatitude();
                    currentLon = lastKnownLocation.getLongitude();
                }
            }
            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 10, locationListener);
    }

    private void getLatLong(String address) {

        try {
            StringBuilder sb = new StringBuilder(GEOCODE_API_BASE + OUT_JSON);
            sb.append("?address=" + URLEncoder.encode(address, "utf-8"));
            sb.append("&key=" + API_KEY);
            System.out.println(sb.toString());

            URL url = new URL(sb.toString());
            new GeocodeAPI(this).execute(url);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void checkForArrival() {

        float[] results;
        results = new float[3];
        Location.distanceBetween(currentLat, currentLon, destLat, destLong, results);
        System.out.println("Distance between points: " + results[0]);

        // Get initDistance, if zero, we are just starting.
        float initDistance = selectedPlace.getInitDistance();
        if(initDistance == 0) {
            selectedPlace.setInitDistance(results[0]);
            calculateProgress(1, selectedPlace.getInitDistance());
        } else {
            calculateProgress(results[0], initDistance);
        }
    }

    private void calculateProgress(float distance, float initDistance) {

        if(distance != 1) {
            float progress = ((initDistance - distance)/initDistance) * 100f;
            if (progress > threshold) {
                ringAlarm();
            }
            sendResultToActivity(progress, distance);
            System.out.println("Progress is: " + progress);
        } else {
            // Ring alarm here for testing purposes because we can't actually move around to increase progress
             //ringAlarm();
            sendResultToActivity(0, initDistance);
        }
    }

    public void sendResultToActivity(float progress, float distance) {

        Intent intent = new Intent(ALARM_RESULT);
        intent.putExtra("progress", progress);
        intent.putExtra("distance", distance);
        int currentTransfer = selectedPlace.getTransfer();
        intent.putExtra("transfer", currentTransfer);
        broadcaster.sendBroadcast(intent);
    }

    private void ringAlarm() {

        boolean isRunning = prefs.getBoolean("isRunning", false);

        if(!isRunning) {
            Intent intent = new Intent(this, DialogActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void processFinish(ArrayList latLng) {

        try {
            JSONObject latLngJSON = new JSONObject(latLng.get(0).toString());
            destLat = latLngJSON.getJSONObject("location").getDouble("lat");
            destLong = latLngJSON.getJSONObject("location").getDouble("lng");

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

