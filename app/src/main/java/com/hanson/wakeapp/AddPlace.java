package com.hanson.wakeapp;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import com.hanson.wakeapp.Place;

public class AddPlace extends AppCompatActivity implements AdapterView.OnItemClickListener {

    DatabaseHelper mDatabaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_place);

        // DB Helper
        mDatabaseHelper = new DatabaseHelper(this);

        // Name of Place
        final EditText placeName = (EditText) findViewById(R.id.placeName);

        // Autocomplete Address
        final AutoCompleteTextView acTextView = (AutoCompleteTextView) findViewById(R.id.placeAddress);
        acTextView.setAdapter(new AutocompleteAdapter(this, R.layout.list_item, R.id.item));
        acTextView.setOnItemClickListener(this);

        // Add Place Button
        Button placeButton = (Button) findViewById(R.id.placeButton);
        placeButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Place mPlace = new Place(placeName.getText().toString(), acTextView.getText().toString(), 0f);
                if (mPlace.name.length() != 0 && mPlace.address.length() != 0) {
                    // Add place to database
                    AddData(mPlace);

                    // Navigate back to MainActivity
                    Intent myIntent = new Intent(view.getContext(), MainActivity.class);
                    startActivity(myIntent);

                } else {
                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Please fill all text fields", Snackbar.LENGTH_LONG );
                    snackbar.show();
                    System.out.println("You must put something in the text fields");
                }
            }
        });
    }

    public void onItemClick(AdapterView adapterView, View view, int position, long id) {
        String str = (String) adapterView.getItemAtPosition(position);
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    public void AddData(Place newEntry) {
        boolean insertData = mDatabaseHelper.addData(newEntry);
        if(insertData) {
            System.out.println("Data inserted successfully");
        } else {
            System.out.println("Something went wrong");
        }
    }

    class AutocompleteAdapter extends ArrayAdapter<String> implements Filterable {

        private ArrayList<String> resultList;

        Context mContext;
        int mResource;
        int mResourceId;

        PlaceAPI mPlaceAPI = new PlaceAPI();

        public AutocompleteAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);

            mContext = context;
            mResource = resource;
            mResourceId = textViewResourceId;
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index){
            return resultList.get(index).toString();
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if(constraint != null) {
                        // Retrieve autocomplete results
                        resultList = mPlaceAPI.autocomplete(constraint.toString());

                        // Assign data to the filter results
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }

                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count>0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }

            };

            return filter;
        }
    }
}
