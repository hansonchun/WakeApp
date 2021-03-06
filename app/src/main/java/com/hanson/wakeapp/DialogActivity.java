package com.hanson.wakeapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class DialogActivity extends Activity {

    Alarm mAlarm;
    Place selectedPlace;

    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    Vibrator vibrator;
    boolean isVibrate;
    Ringtone ringtone;
    Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        mAlarm = new Alarm();
        selectedPlace = mAlarm.mPlace;

        // Get Preferences
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Vibrate preferences
        isVibrate = prefs.getBoolean("vibratePref", true);
        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        // Ringtone preferences
        String alarms = prefs.getString("ringtonePref", "default ringtone");
        uri = Uri.parse(alarms);

        editor = prefs.edit();

        this.setFinishOnTouchOutside(false);

        Button dialogButton = (Button) findViewById(R.id.dialogButton);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAlarmService();
                checkTransfers();
                DialogActivity.this.finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        editor.putBoolean("isRunning", true);
        editor.apply();
        alertUser();
    }

    @Override
    protected void onDestroy() {
        editor.putBoolean("isRunning", false);
        editor.apply();
        super.onDestroy();
    }

    public void startService() {

        Intent serviceIntent = new Intent(this, AlarmService.class);
        startService(serviceIntent);
    }

    public void stopAlarmService() {
        vibrator.cancel();
        ringtone.stop();
        Intent intent = new Intent(this, AlarmService.class);
        stopService(intent);
    }

    public void checkTransfers() {

        int currTransfer = (selectedPlace.addresses.size()-1);
        int transfer = selectedPlace.getTransfer();

        if(currTransfer > transfer) {
            // Reset initial distance
            selectedPlace.incrementTransfer();
            selectedPlace.setInitDistance(0f);
            startService();

        } else {
            returnToMain();
        }
    }

    public void returnToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void alertUser() {

        long[] pattern = {0, 200, 100};

        // Vibrate phone
        if(isVibrate) {
            vibrator.vibrate(pattern, 0);
        }

        // Ringtone
        ringtone = RingtoneManager.getRingtone(this, uri);
        ringtone.play();
    }
}
