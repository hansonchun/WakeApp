package com.hanson.wakeapp;

/**
 * Created by Hanson on 2017-04-14.
 */

public class Place {

    public String name;
    public String address;
    public float initDistance;

    public Place(String name, String address, float initDistance) {

        this.name = name;
        this.address = address;
        this.initDistance = initDistance;
    }

    public float getInitDistance() {
        return initDistance;
    }

    public void setInitDistance(float distance) {
        this.initDistance = distance;
    }
}
