package com.hanson.wakeapp;

import android.os.AsyncTask;
import android.util.ArrayMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


/**
 * Created by Hanson on 2017-04-16.
 */

public class GeocodeAPI extends AsyncTask<URL, Void, String> {

    HttpURLConnection conn;
    public AsyncResponse delegate = null;


    public interface AsyncResponse {
        void processFinish(ArrayList latLong);
    }

    public GeocodeAPI(AsyncResponse delegate) {
        this.delegate = delegate;
    }

    @Override
    protected String doInBackground(URL... params) {

        StringBuilder jsonResults = new StringBuilder();

        try {
            URL url = params[0];
            conn = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(conn.getInputStream());

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while((line = reader.readLine()) != null) {
                jsonResults.append(line);
            }

        } catch (Exception e) {
            System.out.println("ERROR");
            e.printStackTrace();
        } finally {
            conn.disconnect();
        }

        return jsonResults.toString();
    }

    @Override
    protected void onPostExecute(String result) {

        ArrayList latLong = null;

        try {

            JSONObject jsonObj = new JSONObject(result.toString());
            JSONArray results = jsonObj.getJSONArray("results");

            latLong = new ArrayList();

            for(int i=0; i<results.length(); i++) {
                latLong.add(results.getJSONObject(i).getString("geometry"));
            }

        } catch (JSONException e){
            e.printStackTrace();
        }

        delegate.processFinish(latLong);

    }
}
