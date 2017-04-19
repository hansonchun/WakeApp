package com.hanson.wakeapp;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Hanson on 2017-04-17.
 */

public class CustomAdapter extends BaseAdapter implements ListAdapter {

    private ArrayList<String> list = new ArrayList<String>();
    private Context context;
    DatabaseHelper myDatabaseHelper;

    public CustomAdapter(ArrayList<String> list, Context context, DatabaseHelper dbHelper) {
        this.list = list;
        this.context = context;
        this.myDatabaseHelper = dbHelper;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.place_rows, null);
        }

        TextView listItemText = (TextView)view.findViewById(R.id.rowName);
        listItemText.setText(list.get(position));
        final int index = position;

        ImageButton deleteBtn = (ImageButton) view.findViewById(R.id.delete_button);
        deleteBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                deleteFromDB(index);
                list.remove(index);
                notifyDataSetChanged();
            }
        });
        return view;
    }


    public void deleteFromDB(int index) {

        String name = list.get(index);
        System.out.println(name);
        Cursor idData = myDatabaseHelper.getItemID(name);
        int id = -1;
        while(idData.moveToNext()){
            id = idData.getInt(0);
        }
        if(id > -1) {
            myDatabaseHelper.deleteItem(id, name);
        } else {
            System.out.println("No item associated with that ID");
        }
    }


}
