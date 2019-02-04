/* This file is in charge of displaying and modification of dates of a trip */

package com.example.minjielu.tripmanager;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.w3c.dom.Text;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

public class SingleTripActivity extends AppCompatActivity {

    private String title;
    private SQLiteDatabase tripManager;
    private ArrayList<String> info;
    private ArrayList<String> dates;
    private ListView dailyActivities;

    public void scheduleNotification() {//delay is after how much time(in millis) from current time you want to schedule the notification
        tripManager = this.openOrCreateDatabase("TripManager", MODE_PRIVATE, null);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH:mm");
        SimpleDateFormat simpleTotalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date curDateTime = new Date();
        String curDate = simpleDateFormat.format(curDateTime);
        String curTime = simpleTimeFormat.format(curDateTime);
        /* Find the next happening event */
        Cursor cActivity = tripManager.rawQuery("SELECT date, leave FROM activities WHERE legal = 1 and ((date = '" + curDate + "' and leave > '" + curTime + "') or date > '"+ curDate +"') ORDER BY date, leave", null);
        int dateIndex = cActivity.getColumnIndex("date");
        int leaveIndex = cActivity.getColumnIndex("leave");
        cActivity.moveToFirst();
        if(!cActivity.isAfterLast()) {

            String nextDate = cActivity.getString(dateIndex);
            String curLeave = cActivity.getString(leaveIndex);
            Date notificationDate = new Date();
            try {

                notificationDate = simpleTotalFormat.parse(nextDate + " " + curLeave);

            } catch (ParseException e) {

                e.printStackTrace();

            }
            cActivity = tripManager.rawQuery("SELECT rowid, location FROM activities WHERE legal = 1 and ((date = '" + curDate + "' and arrive > '" + curLeave + "') or date > '" + curDate + "') ORDER BY date, arrive LIMIT 1", null);
            cActivity.moveToFirst();
            if(!cActivity.isAfterLast()) {

                int rowId = cActivity.getInt(cActivity.getColumnIndex("rowid"));
                int notificationId = rowId;
                int locationIndex = cActivity.getColumnIndex("location");
                String nextLocation = cActivity.getString(locationIndex);
                /* Build a notification */
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "TripManager")
                        .setContentTitle("Don't be fascinated!")
                        .setContentText("Time to head to " + nextLocation)
                        .setAutoCancel(true)
                        .setSmallIcon(R.mipmap.ic_launcher);


                Notification notification = builder.build();
                /* Send the intent containing the notification to the alarmManager */
                Intent notificationIntent = new Intent(getApplicationContext(), CustomNotificationPublisher.class);
                notificationIntent.putExtra("notification_id", Integer.toString(rowId + 1000));
                notificationIntent.putExtra("notification", notification);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                Calendar cal = Calendar.getInstance();
                cal.setTime(notificationDate);
                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                Toast.makeText(getApplicationContext(),"A Reminder is set", Toast.LENGTH_LONG).show();


            }
        }
    }

    protected Date parseDate(String dateString, String formatString) {

        /* Make sure the date entered is in the right format */
        Date newDate = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat(formatString);

        try {

            // Parse the date to the designated format if it is possible.
            newDate = dateFormat.parse(dateString);


        } catch (Exception e) {

            Toast.makeText(this, "Wrong date format", Toast.LENGTH_LONG).show();
            e.printStackTrace();

        }

        return newDate;

    }

    public void postponeTo(View view) {

        /* After a new starting date is entered by the user, the whole trip will be postponed to start from that date */
        TextView dateView = (TextView) findViewById(R.id.editText7);
        try {

            Date newDate = parseDate(dateView.getText().toString(), "MM-dd-yyyy");

            Cursor cActivity = tripManager.rawQuery("SELECT rowid, * FROM activities WHERE tripname = '" + title + "' ORDER BY date ASC, legal ASC, arrive ASC", null);
            cActivity.moveToFirst();
            int dateIndex = cActivity.getColumnIndex("date"), rowidIndex = cActivity.getColumnIndex("rowid");

            Date oldDate;
            int days = 0;

            /* Calculate the difference between the current starting date and the newly entered starting date */
            if (!cActivity.isAfterLast()) {

                oldDate = parseDate(cActivity.getString(dateIndex), "yyyy-MM-dd");
                long diff = newDate.getTime() - oldDate.getTime();
                days = (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

            }

            /* Update all events belonging to the current trip */
            while (!cActivity.isAfterLast()) {

                String oldDateString = cActivity.getString(dateIndex);
                String rowId = Integer.toString(cActivity.getInt(rowidIndex));
                oldDate = parseDate(oldDateString, "yyyy-MM-dd");
                Calendar cal = Calendar.getInstance();
                cal.setTime(oldDate);
                cal.add(Calendar.DATE, days);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String correctedDate = simpleDateFormat.format(cal.getTime());

                tripManager.execSQL("UPDATE activities SET date = '" + correctedDate + "' WHERE rowid = '" + rowId + "'");

                cActivity.moveToNext();

            }

            // Then the alarmManager for departure should be reset.
            AlarmManager alarmManager = (AlarmManager) this.getSystemService(this.ALARM_SERVICE);
            Intent notificationIntent = new Intent(getApplicationContext(), CustomNotificationPublisher.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplication(),0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.cancel(pendingIntent);

            scheduleNotification();

            // And the list should be refreshed.
            refreshList();

        }
        catch(Exception e) {

            Toast.makeText(getApplicationContext(),"Date format has to be MM-dd-yyyy!", Toast.LENGTH_LONG).show();
            e.printStackTrace();

        }

    }

    public void backToMain(View view) {

        // Back to the Mainactivity of this app.
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);

    }

    public void newDate(View view) {

        /* Add a new date to the current trip */
        String values = "";
        TextView dateView = (TextView) findViewById(R.id.editText3);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
        SimpleDateFormat targetFormat = new SimpleDateFormat("yyyy-MM-dd");

        try {
            // Add the new date if it's in the right format.
            Date date = dateFormat.parse(dateView.getText().toString());
            String dateString = targetFormat.format(date);
            values += "'" + dateString + "', 0, 0, '" + title + "'";
            Log.i("Date", values);
            tripManager.execSQL("INSERT INTO activities (date, legal, number, tripname) VALUES (" + values + ")");
            Toast.makeText(this, "Date added", Toast.LENGTH_LONG).show();
            // Refresh the list afterwards.
            refreshList();


        } catch (Exception e) {

            Toast.makeText(this, "Wrong date format", Toast.LENGTH_LONG).show();
            e.printStackTrace();

        }



    }

    protected void refreshList() {

        /* Refresh the list of dates */
        Cursor cActivity = tripManager.rawQuery("SELECT rowid, * FROM activities WHERE tripname = '" + title + "' ORDER BY date ASC, legal ASC, arrive ASC", null);

        dates = new ArrayList<String>();
        info = new ArrayList<String>();
        int cnt = 1;
        int dateIndex = cActivity.getColumnIndex("date"), locationIndex = cActivity.getColumnIndex("location"), legalIndex = cActivity.getColumnIndex("legal"),
                rowidIndex = cActivity.getColumnIndex("rowid"), numberIndex = cActivity.getColumnIndex("number");
        cActivity.moveToFirst();
        while (!cActivity.isAfterLast()) {

            String date = cActivity.getString(dateIndex);
            dates.add(date);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat targetFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

            try {

                date = targetFormat.format(dateFormat.parse(date));

            } catch (ParseException e) {

                e.printStackTrace();

            }

            String daily = "Day" + Integer.toString(cnt++) + " (" + date + "): ";

            if (cActivity.getInt(numberIndex) == 0) {

                daily += "No activity";
                cActivity.moveToNext();

            } else {

                int lastLegalRowid = 0, firstLegalRowid = 0;
                String lastLegalLocation = "";
                cActivity.moveToNext();

                /* Each date entry includes the date and the starting and ending location of this date */
                while (!cActivity.isAfterLast() && cActivity.getInt(legalIndex) != 0) {

                    if (cActivity.getInt(legalIndex) == 1) {

                        if (firstLegalRowid == 0) {

                            daily += "\n" + cActivity.getString(locationIndex);
                            lastLegalRowid = cActivity.getInt(rowidIndex);
                            firstLegalRowid = cActivity.getInt(rowidIndex);

                        } else {

                            lastLegalLocation = cActivity.getString(locationIndex);
                            lastLegalRowid = cActivity.getInt(rowidIndex);

                        }

                    }
                    cActivity.moveToNext();

                }

                if (lastLegalRowid != firstLegalRowid) {

                    daily += "  ->  " + lastLegalLocation;

                }

            }

            info.add(daily);

        }

        /* Update the list using an ArrayList */
        dailyActivities = (ListView) findViewById(R.id.ListView2);
        if (info.size() == 0) {

            ArrayList<String> emptyList = new ArrayList<String>(asList("No activity"));
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, emptyList);
            dailyActivities.setAdapter(adapter);

        } else {

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, info);
            dailyActivities.setAdapter(adapter);

        }
    }

    public void deleteTrip(View view) {

        /* Delete the whole trip */
        TextView deletingTrip = (TextView) findViewById(R.id.editText);
        String oldTripName = deletingTrip.getText().toString();

        /* The name of the trip has to be entered correctly case sensitive to delete it */
        if(oldTripName.equals(title)) {

            new AlertDialog.Builder(this)
                    .setTitle("Warning").setMessage("Do you really want to delete this trip?")
                    .setIcon(android.R.drawable.alert_light_frame)
                    .setPositiveButton("yes", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            /* Delete the whole trip if its name is entered correctly */
                            tripManager = getApplicationContext().openOrCreateDatabase("TripManager", MODE_PRIVATE, null);
                            tripManager.execSQL("DELETE FROM trips WHERE tripname = '" + title + "'");
                            tripManager.execSQL("DELETE FROM activities WHERE tripname = '" + title + "'");
                            Toast.makeText(getApplicationContext(), "Trip deleted", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                            startActivity(intent);

                        }

                    })
                    .setNegativeButton("no", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            Toast.makeText(getApplicationContext(), "Deletion cancelled", Toast.LENGTH_LONG).show();

                        }

                    }).show();

        }
        else {

            Toast.makeText(this, "Entered name doesn't match trip name", Toast.LENGTH_LONG).show();

        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_trip);

        TextView titleView = (TextView) findViewById(R.id.textView);
        Intent oldIntent = getIntent();
        title = oldIntent.getStringExtra("tripname");
        titleView.setText(title);

        tripManager = this.openOrCreateDatabase("TripManager", MODE_PRIVATE, null);
        tripManager.execSQL("CREATE TABLE IF NOT EXISTS activities (date TEXT, arrive TEXT, leave TEXT, location TEXT, " +
                "latitude TEXT, longitude TEXT, activity TEXT, tripname TEXT, legal INT(1), number INT(1), FOREIGN KEY(tripname) REFERENCES trips(tripname))");

        refreshList();

        /* Each date in the list is assigned a OnItemClickListener to redirect to the activity for that specific date */
        dailyActivities.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if(dates.size() != 0) {

                    String date = dates.get(position);
                    Intent intent = new Intent(getApplicationContext(), SingleActivityActivity.class);
                    intent.putExtra("tripname", title);
                    intent.putExtra("date", date);
                    startActivity(intent);

                }

            }


        });
    }
}
