package com.hanson.wakeapp;

import android.*;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;


public class Alarm extends AppCompatActivity {

    BroadcastReceiver receiver;

    private String selectedName;
    private int selectedID;

    TextView tvDestination;
    TextView tvDistance;
    TextView tvProgress;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        // Get intent extras
        Intent receivedIntent = getIntent();
        selectedID = receivedIntent.getIntExtra("id", -1);
        selectedName = receivedIntent.getStringExtra("name");

        // Initialize views
        tvDestination = (TextView) findViewById(R.id.destination);
        tvDistance = (TextView) findViewById(R.id.distance);
        tvProgress = (TextView) findViewById(R.id.progress);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setScaleY(3f);

        Button alarmServiceBtn = (Button) findViewById(R.id.startAlarm);
        alarmServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(v);
            }

        });

        // Receive information from AlarmService
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                System.out.println("onReceive reached");

                float progress = intent.getFloatExtra("progress", 0);
                float distance = intent.getFloatExtra("distance", 0);
                distance = distance/1000.0f;

                tvDistance.setText("Distance: " + String.format("%.2f", distance) + " km");
                tvProgress.setText("Progress: " + Integer.toString(Math.round(progress)) + "%");
                progressBar.setProgress(Math.round(progress));
            }
        };
    }

    public void startService(View view) {
        Intent serviceIntent = new Intent(this, AlarmService.class);
        serviceIntent.putExtra("selectedName", selectedName);
        serviceIntent.putExtra("selectedID", selectedID);
        startService(serviceIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        tvDestination.setText(selectedName);
        requestLocationPermissions();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver), new IntentFilter(AlarmService.ALARM_RESULT));
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Intent serviceIntent = new Intent(this, AlarmService.class);
        stopService(serviceIntent);
        System.out.println("Alarm Service stopped.");
        super.onDestroy();
    }

    private void requestLocationPermissions() {
        System.out.println("Requesting for permissions...");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }


}
