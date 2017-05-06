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
    public Place mPlace;
    public int selectedID;
    public String selectedName;

    LocalBroadcastManager broadcaster;

    public float threshold = 80;
    Vibrator vibrator;

    Context context;

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    @Override
    public void onCreate() {
        context = this;
        mDatabaseHelper = new DatabaseHelper(this);
        broadcaster = LocalBroadcastManager.getInstance(this);
        vibrator = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
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

        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        selectedID = intent.getIntExtra("selectedID", 0);
        selectedName = intent.getStringExtra("selectedName");

        // Retrieve place object
        mPlace = retrievePlace(selectedID, selectedName);
        System.out.println("Place name is : " + mPlace.name + " and place address is " + mPlace.address);

        // Get destination lat and long
        getLatLong();

        // Start listening for location updates
        startListener();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        locationManager.removeUpdates(locationListener);
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
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

                Toast.makeText(getBaseContext(), currentLat + "-" + currentLon, Toast.LENGTH_SHORT).show();
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

    public Place retrievePlace(int id, String name) {

        // Get Place object with item ID and name
        Cursor placeData = mDatabaseHelper.getObjectFromID(id, name);
        Place mPlace = null;
        while(placeData.moveToNext()) {
            mPlace = new Place(placeData.getString(1), placeData.getString(2), 0f);
        }
        return mPlace;
    }

    private void getLatLong() {

        try {
            StringBuilder sb = new StringBuilder(GEOCODE_API_BASE + OUT_JSON);
            sb.append("?address=" + URLEncoder.encode(mPlace.address, "utf-8"));
            sb.append("&key=" + API_KEY);
            System.out.println(sb.toString());

            URL url = new URL(sb.toString());
            new GeocodeAPI(this).execute(url);

        } catch (Exception e) {
            e.printStackTrace();
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

    private void checkForArrival() {

        float[] results;
        results = new float[3];
        Location.distanceBetween(currentLat, currentLon, destLat, destLong, results);
        System.out.println("Distance between points: " + results[0]);

        // Get initDistance, if zero, we are just starting.
        float initDistance = mPlace.getInitDistance();
        if(initDistance == 0) {
            mPlace.setInitDistance(results[0]);
            calculateProgress(1, mPlace.getInitDistance());
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
            ringAlarm();
            sendResultToActivity(0, initDistance);
        }
    }

    public void sendResultToActivity(float progress, float distance) {

        Intent intent = new Intent(ALARM_RESULT);
        intent.putExtra("progress", progress);
        intent.putExtra("distance", distance);
        broadcaster.sendBroadcast(intent);
    }

    private void ringAlarm() {

        Toast.makeText(this, "WE HAVE ARRIVED.", Toast.LENGTH_SHORT).show();
        boolean isRunning = prefs.getBoolean("isRunning", false);

        if(!isRunning) {
            Intent intent = new Intent(this, DialogActivity.class);
            startActivity(intent);
        }

    }
}

