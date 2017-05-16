package com.hanson.wakeapp;

import android.*;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class Alarm extends AppCompatActivity {

    public static Place mPlace;

    BroadcastReceiver receiver;

    DatabaseHelper mDatabaseHelper;

    private String selectedName;
    private int selectedID;

    TextView tvDestination;
    TextView tvDistance;
    TextView tvProgress;
    ProgressBar progressBar;
    LinearLayout transferButtonsLayout;

    ImageButton[] transferButtons;
    int[] bgResourcesOff = {R.mipmap.ic_transfer_1_off, R.mipmap.ic_transfer_2_off, R.mipmap.ic_transfer_3_off, R.mipmap.ic_transfer_4_off};
    int[] bgResourcesOn = {R.mipmap.ic_transfer_1_on, R.mipmap.ic_transfer_2_on, R.mipmap.ic_transfer_3_on, R.mipmap.ic_transfer_4_on};

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
        progressBar.setVisibility(View.INVISIBLE);
        progressBar.setScaleY(3f);
        final TextView alarmText = (TextView) findViewById(R.id.alarmText);

        mDatabaseHelper = new DatabaseHelper(this);

        // Retrieve place data
        mPlace = retrievePlace(selectedID, selectedName);

        // Set up Start Trip button
        final ImageButton alarmServiceBtn = (ImageButton) findViewById(R.id.startAlarm);
        alarmServiceBtn.setTag(1);
        alarmServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int status = (Integer) v.getTag();
                if(status == 1) {
                    // Start the service
                    startService();
                    alarmServiceBtn.setBackgroundResource(R.mipmap.ic_stoptrip);
                    alarmText.setText("Stop Trip");
                    v.setTag(0);
                } else {
                    // Stop the service
                    stopService(v);
                    alarmServiceBtn.setBackgroundResource(R.mipmap.ic_starttrip);
                    alarmText.setText("Start Trip");
                    v.setTag(1);
                }
            }
        });

        // Set up Settings button
        final ImageButton settingsBtn = (ImageButton) findViewById(R.id.settingsButton);
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });

        // Set up transfer buttons
        transferButtonsLayout = (LinearLayout) findViewById(R.id.transferButtonLayout);
        createTransferButtons();

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
                setTransferButton();

                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(Math.round(progress));
            }
        };
    }

    public Place retrievePlace(int id, String name) {

        // Get Place object with item ID and name
        Cursor placeData = mDatabaseHelper.getObjectFromID(id, name);
        Place mPlace = null;
        ArrayList<String> addressArray = new ArrayList<>();
        while(placeData.moveToNext()) {
            String addressString = placeData.getString(2);
            try {
                JSONObject json = new JSONObject(addressString);
                JSONArray jsonArray = json.optJSONArray("addresses");
                for(int i=0; i<jsonArray.length(); i++) {
                    addressArray.add(jsonArray.optString(i));
                }

            } catch(JSONException e) {
                e.printStackTrace();
            }
            mPlace = new Place(placeData.getString(1), addressArray, 0f, 0);
        }
        return mPlace;
    }

    public void startService() {

        Intent serviceIntent = new Intent(this, AlarmService.class);
        setTransferButton();
        startService(serviceIntent);
    }

    public void stopService(View view) {
        Intent serviceIntent = new Intent(this, AlarmService.class);
        stopService(serviceIntent);
    }

    public void openSettings() {
        Intent settingsIntent = new Intent(this, Settings.class);
        startActivity(settingsIntent);
    }

    public void createTransferButtons() {

        if(transferButtonsLayout.getChildCount() > 0) {
            transferButtonsLayout.removeAllViews();
        }

        int numOfTransfers = (mPlace.addresses.size());
        transferButtons = new ImageButton[numOfTransfers];

        for(int i=0; i<numOfTransfers; i++) {

            final ImageButton transferButton = new ImageButton(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(30, 20, 30, 10);
            transferButtonsLayout.setGravity(Gravity.CENTER);

            transferButton.setBackgroundResource(bgResourcesOff[i]);
            transferButton.setPadding(10, 10, 10, 10);

            transferButtonsLayout.addView(transferButton);
            transferButtons[i] = transferButton;
        }
    }

    public void setTransferButton() {

        // Reset all buttons to off
        for(int i=0; i<transferButtons.length; i++) {
            transferButtons[i].setBackgroundResource(bgResourcesOff[i]);
        }
        // Set current transfer to on
        int currTransfer = mPlace.getTransfer();
        System.out.println("Current Transfer:" + currTransfer);
        transferButtons[currTransfer].setBackgroundResource(bgResourcesOn[currTransfer]);
    }


    @Override
    protected void onStart() {
        super.onStart();
        tvDestination.setText("Destination: " + selectedName);
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
