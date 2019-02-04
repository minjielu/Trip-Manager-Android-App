/* This file is in charge of displaying and modification of events in a specified date */

package com.example.minjielu.tripmanager;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import static java.util.Arrays.asList;

public class SingleActivityActivity extends AppCompatActivity {

    private String title;
    private String date;
    private SQLiteDatabase tripManager; // The database for this app.
    private ArrayList<Integer> itemId;
    private ArrayList<String> itemInfo;

    public void viewRoute(View view) {
        /* Redirect to the google map page to view daily route */
        Intent intent = new Intent(getApplicationContext(), MapRouteActivity.class);
        intent.putExtra("tripname", title);
        intent.putExtra("date", date);
        startActivity(intent);

    }

    public void backToTrip(View view) {

        /* Back to the upper activity that display all dates of a trip */
        Intent intent = new Intent(getApplicationContext(), SingleTripActivity.class);
        intent.putExtra("tripname", title);
        startActivity(intent);

    }

    protected void refreshList() {
        /* After some action, like an event is added or deleted, refresh the activity */

        Cursor cActivity = tripManager.rawQuery("SELECT rowid, * FROM activities WHERE tripname = '" + title + "' and date = '" + date + "' ORDER BY arrive", null);

        itemId = new ArrayList<Integer>();
        itemInfo = new ArrayList<String>();
        int arriveIndex = cActivity.getColumnIndex("arrive"), leaveIndex = cActivity.getColumnIndex("leave"),
                locationIndex = cActivity.getColumnIndex("location"), activityIndex = cActivity.getColumnIndex("activity"),
                rowidIndex = cActivity.getColumnIndex("rowid"), legalIndex = cActivity.getColumnIndex("legal");
        cActivity.moveToFirst();
        while (!cActivity.isAfterLast()) {

            if(cActivity.getInt(legalIndex) != 0) {

                itemId.add(cActivity.getInt(rowidIndex));

                String arrive = cActivity.getString(arriveIndex);
                String leave = cActivity.getString(leaveIndex);
                String location = cActivity.getString(locationIndex);
                String activity = cActivity.getString(activityIndex);

                itemInfo.add(arrive + "-" + leave + "  " + location + '\n' + activity);

            }

            cActivity.moveToNext();

        }

        /* Modify the list using an ArrayList */
        ListView dailyActivities = (ListView) findViewById(R.id.ListView6);
        if (itemId.size() == 0) {

            ArrayList<String> emptyList = new ArrayList<String>(asList("No activity"));
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, emptyList);
            dailyActivities.setAdapter(adapter);

        } else {

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, itemInfo);
            dailyActivities.setAdapter(adapter);

        }


    }

    public void deleteAll(View view) {

        /* Delete this date */
        // Ask for confirmation.
        new AlertDialog.Builder(this)
                .setTitle("Warning").setMessage("Do you really want to delete all items of this date?")
                .setIcon(android.R.drawable.alert_light_frame)
                .setPositiveButton("yes", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // If yes, delete this date.
                        tripManager = getApplicationContext().openOrCreateDatabase("TripManager", MODE_PRIVATE, null);
                        tripManager.execSQL("DELETE FROM activities WHERE tripname = '" + title + "' and date = '" + date + "'");
                        Toast.makeText(getApplicationContext(), "Items deleted", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(getApplicationContext(), SingleTripActivity.class);
                        intent.putExtra("tripname", title);
                        startActivity(intent);

                    }

                })
                .setNegativeButton("no", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // If no, cancel the deletion.
                        Toast.makeText(getApplicationContext(), "Deletion cancelled", Toast.LENGTH_LONG).show();

                    }

                }).show();

    }

    public void newActivity(View view) {

        /* Go to the create event activity to create a new event */
        Intent intent = new Intent(getApplicationContext(), CreateActivity.class);
        intent.putExtra("Fun", "Create");
        intent.putExtra("tripname", title);
        intent.putExtra("date", date);
        startActivity(intent);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_activity);

        tripManager = this.openOrCreateDatabase("TripManager", MODE_PRIVATE, null);

        TextView titleView = (TextView) findViewById(R.id.textView6);
        Intent oldIntent = getIntent();
        title = oldIntent.getStringExtra("tripname");
        titleView.setText(title);
        titleView = (TextView) findViewById(R.id.textView2);
        date = oldIntent.getStringExtra("date");
        String newDate = "";
        /* Format the date and time from the form input by the user to the form stored in the database */
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat targetFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

        try {

            newDate = targetFormat.format(dateFormat.parse(date));

        } catch (ParseException e) {

            e.printStackTrace();

        }

        titleView.setText(newDate);

        tripManager = this.openOrCreateDatabase("TripManager", MODE_PRIVATE, null);

        refreshList();

        ListView dailyActivities = (ListView) findViewById(R.id.ListView6);

        /* For each date in the list, set up an onClickListener to redirect to the activity of that date */
        dailyActivities.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if(itemInfo.size() != 0) {

                    String item = Integer.toString(itemId.get(position));
                    Intent intent = new Intent(getApplicationContext(), CreateActivity.class);
                    intent.putExtra("tripname", title);
                    intent.putExtra("date", date);
                    intent.putExtra("itemid", item);
                    intent.putExtra("Fun","Update");
                    startActivity(intent);

                }

            }


        });

    }
}
