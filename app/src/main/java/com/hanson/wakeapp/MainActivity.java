package com.hanson.wakeapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ListView mListView;

    DatabaseHelper mDatabaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Add place button
        FloatingActionButton newPlaceButton = (FloatingActionButton) findViewById(R.id.addPlace);
        newPlaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(view.getContext(), AddPlace.class);
                startActivityForResult(myIntent, 0);
            }
        });

        // ListView
        mListView = (ListView) findViewById(R.id.placesList);
        mDatabaseHelper = new DatabaseHelper(this);

        populateListView();
    }

    private void populateListView() {
        Log.d(TAG, "Displaying data in the list view");

        Cursor data = mDatabaseHelper.getData();
        ArrayList<String> listData = new ArrayList<>();
        while(data.moveToNext()){
            listData.add(data.getString(1));
        }
        // Custom Adapter
        CustomAdapter customAdapter = new CustomAdapter(listData, this, mDatabaseHelper);
        mListView.setAdapter(customAdapter);

        // ListView OnItemClickListener
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String name = adapterView.getItemAtPosition(i).toString();

                // Get item ID
                Cursor idData = mDatabaseHelper.getItemID(name);
                int itemID = -1;
                while(idData.moveToNext()){
                    itemID = idData.getInt(0);
                }

                if(itemID > -1) {
                    // Attach id and name to intent and send to Alarm Activity
                    Intent alarmIntent = new Intent(view.getContext(), Alarm.class);
                    alarmIntent.putExtra("id", itemID);
                    alarmIntent.putExtra("name", name);
                    startActivity(alarmIntent);
                } else {
                    System.out.println("No ID associated with that name");
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
