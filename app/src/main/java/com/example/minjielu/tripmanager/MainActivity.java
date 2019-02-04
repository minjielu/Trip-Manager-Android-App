/* This is the main activity of the app displaying all trips */

package com.example.minjielu.tripmanager;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static java.util.Arrays.asList;

public class MainActivity extends AppCompatActivity {

    private ListView trips;
    private SQLiteDatabase tripManager;
    private ArrayList<String> tripNames;

    public void newTrip(View view) {

        /* Add a new trip by name */
        TextView nameEntered = (TextView) findViewById(R.id.editText2);
        String tripName = nameEntered.getText().toString();
        if(tripName.equals("")) {
            // Cancel if nothing is entered.
            Toast.makeText(this,"Please enter a trip name", Toast.LENGTH_LONG).show();

        }
        else {

            boolean repeated = false;
            // Check if the trip with the same name already exists.
            for(String oldName : tripNames) {

                if(oldName.equals(tripName)) {

                    repeated = true;
                    break;

                }

            }

            if(repeated) {

                Toast.makeText(this,tripName + " already exists", Toast.LENGTH_LONG).show();

            }
            else {
                // Otherwise create a dummy event for the trip in the database.
                tripManager.execSQL("INSERT INTO trips (tripname) VALUES ('" + tripName + "')");
                tripNames.add(tripName);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, tripNames);
                trips.setAdapter(adapter);

            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Open the TripManager database if it already exists, otherwise create it.
        tripManager = this.openOrCreateDatabase("TripManager", MODE_PRIVATE, null);
        trips = (ListView) findViewById(R.id.trips);

        tripManager.execSQL("CREATE TABLE IF NOT EXISTS trips (tripname TEXT)");
        Cursor cTrip = tripManager.rawQuery("SELECT tripname FROM trips", null);

        /* Display all trips in a list using ArrayList */
        cTrip.moveToFirst();
        tripNames = new ArrayList<String>();
        int nameIndex = cTrip.getColumnIndex("tripname");
        while(!cTrip.isAfterLast()) {
            tripNames.add(cTrip.getString(nameIndex));
            cTrip.moveToNext();
        }

        if(tripNames.size() != 0) {

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, tripNames);
            trips.setAdapter(adapter);

        }
        else {

            ArrayList<String> emptylist = new ArrayList<String>(asList("No available plan"));
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, emptylist);
            trips.setAdapter(adapter);

        }

        /* For each trip in the list set a ClickListener that redirects to the activity that displays details about that trip */
        trips.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if(tripNames.size() != 0) {

                    String tripName = tripNames.get(position);
                    Intent intent = new Intent(getApplicationContext(), SingleTripActivity.class);
                    intent.putExtra("tripname", tripName);
                    startActivity(intent);

                }

            }


        });

    }
}
