package com.hanson.wakeapp;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.preference.PreferenceFragment;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import com.hanson.wakeapp.Place;

public class AddPlace extends AppCompatActivity implements AdapterView.OnItemClickListener {

    DatabaseHelper mDatabaseHelper;

    LinearLayout mLinearLayout;

    AutoCompleteTextView[] acTextViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_place);

        // DB Helper
        mDatabaseHelper = new DatabaseHelper(this);

        mLinearLayout = (LinearLayout) findViewById(R.id.transfersLayout);

        // Name of Place
        final EditText placeName = (EditText) findViewById(R.id.placeName);

        // Spinner
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.transfer_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int numOfTransfers = parent.getSelectedItemPosition();
                createAutocompleteTextViews(numOfTransfers + 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        // Add Place Button
        Button placeButton = (Button) findViewById(R.id.placeButton);
        placeButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                ArrayList<String> stringArray = convertToStringArray(acTextViews);

                Place mPlace = new Place(placeName.getText().toString(), stringArray, 0f, 0);
                if (mPlace.name.length() != 0 && !stringArray.isEmpty()) {
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

    public void createAutocompleteTextViews(int transfers) {

        // Clear all views
        if(mLinearLayout.getChildCount() > 0) {
            mLinearLayout.removeAllViews();
        }

        Resources r = getResources();
        acTextViews = new AutoCompleteTextView[transfers];

        for(int i = 0; i<transfers; i++) {
            final AutoCompleteTextView rowACTextView = new AutoCompleteTextView(this);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(30, 10, 30, 10);

            rowACTextView.setBackgroundResource(R.drawable.tvborder);
            rowACTextView.setEms(8);
            rowACTextView.setHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 55, r.getDisplayMetrics()));
            rowACTextView.setPadding(30, 30, 30, 30);
            rowACTextView.setHint("Address " + (i + 1));
            rowACTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_place_black_24dp, 0, 0, 0);
            rowACTextView.setCompoundDrawablePadding(12);
            rowACTextView.setLayoutParams(params);
            rowACTextView.setAdapter(new AutocompleteAdapter(this, R.layout.list_item, R.id.item));
            rowACTextView.setOnItemClickListener(this);

            mLinearLayout.addView(rowACTextView);
            acTextViews[i] = rowACTextView;
        }

    }

    public ArrayList<String> convertToStringArray(AutoCompleteTextView[] acArray) {

        ArrayList<String> stringArray = new ArrayList<>();

        for(int i = 0; i<acArray.length; i++) {
            String item = acArray[i].getText().toString();
            stringArray.add(item);
        }
        return stringArray;
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
