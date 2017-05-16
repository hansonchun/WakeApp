package com.hanson.wakeapp;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Hanson on 2017-04-14.
 */

public class Place {

    public String name;
    ArrayList<String> addresses;
    private float initDistance;
    private int currentTransfer;


    Place(String name, ArrayList<String> addresses, float initDistance, int currentTransfer){

        this.name = name;
        this.addresses = addresses;
        this.initDistance = initDistance;
        this.currentTransfer = currentTransfer;
    }

    float getInitDistance() {
        return initDistance;
    }

    void setInitDistance(float distance) {
        this.initDistance = distance;
    }

    int getTransfer() {
        return currentTransfer;
    }

    void incrementTransfer() {
        currentTransfer++;
    }


}
