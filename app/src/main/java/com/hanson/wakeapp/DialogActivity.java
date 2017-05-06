package com.hanson.wakeapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class DialogActivity extends Activity {

    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();

        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        Button dialogButton = (Button) findViewById(R.id.dialogButton);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop the Alarm service
                stopAlarmService();
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

    public void stopAlarmService() {
        vibrator.cancel();
        Intent intent = new Intent(this, AlarmService.class);
        stopService(intent);
    }

    public void alertUser() {
        long[] pattern = {0, 200, 100};
        vibrator.vibrate(pattern, 0);
    }
}
