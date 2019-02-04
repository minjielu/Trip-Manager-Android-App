/* This file creates or edits an event */

package com.example.minjielu.tripmanager;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class CreateActivity extends AppCompatActivity {

    private String title; // Title of the trip.
    private String date; // Date of the trip.
    private String latitude; // Coordinate where the event happens.
    private String longitude;
    private SQLiteDatabase tripManager;

    public void scheduleNotification() {//delay is after how much time(in millis) from current time you want to schedule the notification
        /* Find the next happening event */
        /* Formatting date and time */
        tripManager = this.openOrCreateDatabase("TripManager", MODE_PRIVATE, null);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH:mm");
        SimpleDateFormat simpleTotalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date curDateTime = new Date();
        String curDate = simpleDateFormat.format(curDateTime);
        String curTime = simpleTimeFormat.format(curDateTime);

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
                Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
                stackBuilder.addNextIntentWithParentStack(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                /* Build a push Notification */
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "TripManager")
                        .setContentTitle("Don't be fascinated!")
                        .setContentText("Time to head to " + nextLocation)
                        .setAutoCancel(true)
                        .setContentIntent(resultPendingIntent)
                        .setSmallIcon(R.mipmap.ic_launcher);


                Notification notification = builder.build();

                Intent notificationIntent = new Intent(getApplicationContext(), CustomNotificationPublisher.class);
                notificationIntent.putExtra("notification_id", Integer.toString(rowId));
                notificationIntent.putExtra("notification", notification);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                /* register the notification in the alarmManager */
                AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                Calendar cal = Calendar.getInstance();
                cal.setTime(notificationDate);
                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);


            }
        }
    }

    public void locateOnMap(View view) {
        /* Link to the activity that allows users to locate the event on a google map by searching */
        String arrive = ((TextView) findViewById(R.id.editText4)).getText().toString();
        String leave = ((TextView) findViewById(R.id.editText12)).getText().toString();
        String location = ((TextView) findViewById(R.id.editText13)).getText().toString();
        String activity = ((TextView) findViewById(R.id.editText5)).getText().toString();

        Intent intent = new Intent(getApplicationContext(), LocateMapActivity.class);
        /* all information about the event should be passed to the map activity so that we can come back */
        intent.putExtra("arrive", arrive);
        intent.putExtra("leave", leave);
        intent.putExtra("location", location);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        intent.putExtra("activity", activity);
        intent.putExtra("tripname", title);
        intent.putExtra("date", date);
        Intent oldIntent = getIntent();
        if(oldIntent.getStringExtra("Fun").equals("Update")) {

            intent.putExtra("itemid", oldIntent.getStringExtra("itemid"));
            intent.putExtra("Fun", "Update");

        }
        else {

            intent.putExtra("Fun", "Create");

        }
        startActivity(intent);

    }

    public void deleteItem(View view) {

        /* Delete an event */
        Intent oldIntent = getIntent();
        tripManager.execSQL("DELETE FROM activities WHERE rowid = " + oldIntent.getStringExtra("itemid"));
        tripManager.execSQL("UPDATE activities SET number = number - 1 WHERE tripname = '" + title + "' and legal = 0 and date = '" + date + "'");
        Intent intent = new Intent(getApplicationContext(), SingleActivityActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear the stack for the back button.
        intent.putExtra("tripname", title);
        intent.putExtra("date", date);
        startActivity(intent);

    }

    public void backToDay(View view) {

        /* Back to the upper activity that shows agenda for a single day */
        Intent intent = new Intent(getApplicationContext(), SingleActivityActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear the stack for the back button.
        intent.putExtra("tripname", title);
        intent.putExtra("date", date);
        startActivity(intent);

    }

    public void saveActivity(View view) {

        /* Save the current activity that the user is trying to add */
        String arrive = ((TextView) findViewById(R.id.editText4)).getText().toString();
        String leave = ((TextView) findViewById(R.id.editText12)).getText().toString();
        String location = ((TextView) findViewById(R.id.editText13)).getText().toString();
        String activity = ((TextView) findViewById(R.id.editText5)).getText().toString();
        Intent oldIntent = getIntent();

        boolean canSave = true;
        String pattern = "([0,1][0-9]|[2][0-3]):[0-5][0-9]"; // Make sure the arrive and leave time is valid
        if(!arrive.matches(pattern)) {

            canSave = false;
            Toast.makeText(getApplicationContext(), "Arrive time must be in the form of hh:mm!", Toast.LENGTH_LONG).show();

        }
        if(!leave.matches(pattern)) {

            canSave = false;
            Toast.makeText(getApplicationContext(), "Leave time must be in the form of hh:mm!", Toast.LENGTH_LONG).show();

        }
        if(arrive.compareTo(leave) > 0) {

            canSave = false;
            Toast.makeText(getApplicationContext(), "Arrive time must be earlier than leave time!", Toast.LENGTH_LONG).show();

        }

        /* Make sure the current adding event doesn't overlapped with existing events */
        Cursor cActivity = tripManager.rawQuery("SELECT rowid, arrive, leave FROM activities WHERE tripname = '" + title + "' and date = '" + date + "' and legal = 1", null);
        cActivity.moveToFirst();
        int arriveIndex = cActivity.getColumnIndex("arrive"), leaveIndex = cActivity.getColumnIndex("leave"), rowidIndex = cActivity.getColumnIndex("rowid");
        while(!cActivity.isAfterLast()) {

            String curArrive = cActivity.getString(arriveIndex);
            String curLeave = cActivity.getString(leaveIndex);
            if((arrive.compareTo(curArrive) > 0 && arrive.compareTo(curLeave) < 0) || (leave.compareTo(curArrive) > 0 && leave.compareTo(curLeave) < 0)
                    || (curArrive.compareTo(arrive) > 0 && curArrive.compareTo(leave) < 0) || (curLeave.compareTo(arrive) > 0 && curLeave.compareTo(leave) < 0)) {
                if((!oldIntent.getStringExtra("Fun").equals("Update") && !oldIntent.getStringExtra("Fun").equals("UpdateLocated")) || !oldIntent.getStringExtra("itemid").equals(Integer.toString(cActivity.getInt(rowidIndex)))) {

                    canSave = false;
                    Toast.makeText(getApplicationContext(), "Overlapped time", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(getApplicationContext(), SingleActivityActivity.class);
                    intent.putExtra("tripname", title);
                    intent.putExtra("date", date);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                }

            }
            cActivity.moveToNext();
        }

        if(canSave) {


            /* Save the current event */
            String values = "'" + date + "', '" + arrive + "', '" + leave + "', '" + location + "', '" + latitude + "', '" +
                    longitude + "', '" + activity + "', '" + title + "', 1, 0";
            tripManager.execSQL("INSERT INTO activities VALUES (" + values + ")");

            if(oldIntent.getStringExtra("Fun").equals("Update") || oldIntent.getStringExtra("Fun").equals("UpdateLocated")) {

                tripManager.execSQL("DELETE FROM activities WHERE rowid = " + oldIntent.getStringExtra("itemid"));

            }
            tripManager.execSQL("UPDATE activities SET number = number + 1 WHERE tripname = '" + title + "' and date = '" + date + "' and legal = 0");
            AlarmManager alarmManager = (AlarmManager) this.getSystemService(this.ALARM_SERVICE);
            Intent notificationIntent = new Intent(getApplicationContext(), CustomNotificationPublisher.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplication(),0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.cancel(pendingIntent);

            scheduleNotification();

            Intent intent = new Intent(getApplicationContext(), SingleActivityActivity.class);
            intent.putExtra("tripname", title);
            intent.putExtra("date", date);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        Intent oldIntent = getIntent();
        String fun = oldIntent.getStringExtra("Fun");
        TextView titleView = (TextView) findViewById(R.id.textView);

        title = oldIntent.getStringExtra("tripname");
        date = oldIntent.getStringExtra("date");
        titleView.setText(title);
        titleView = (TextView) findViewById(R.id.textView2);
        String newDate = "";

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat targetFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

        try {

            newDate = targetFormat.format(dateFormat.parse(date));

        } catch (ParseException e) {

            e.printStackTrace();

        }

        titleView.setText(newDate);
        tripManager = this.openOrCreateDatabase("TripManager", MODE_PRIVATE, null);

        latitude = "";
        longitude = "";

        /* There are three possible paths to reach this createActivity page,
        1. Create an event.
        2. Edit an event.
        3. After user locate the event on the google map.
        For the 1. and sometimes for the 3. case, we don't need a delete button */
        Button deleteButton = (Button) findViewById(R.id.button6);
        deleteButton.setVisibility(View.INVISIBLE);

        if(fun.equals("Update")) {
            /* Edit an event */

            deleteButton.setVisibility(View.VISIBLE);

            Cursor cActivity = tripManager.rawQuery("SELECT * FROM activities WHERE rowid = " + oldIntent.getStringExtra("itemid") + " LIMIT 1", null);
            int arriveIndex = cActivity.getColumnIndex("arrive"), leaveIndex = cActivity.getColumnIndex("leave"),
                    locationIndex = cActivity.getColumnIndex("location"), activityIndex = cActivity.getColumnIndex("activity"),
                    latitudeIndex = cActivity.getColumnIndex("latitude"), longitudeIndex = cActivity.getColumnIndex("longitude");
            cActivity.moveToFirst();

            TextView arrive = ((TextView) findViewById(R.id.editText4));
            TextView leave = ((TextView) findViewById(R.id.editText12));
            TextView location = ((TextView) findViewById(R.id.editText13));
            TextView activity = ((TextView) findViewById(R.id.editText5));
            TextView coordinate = (TextView) findViewById(R.id.textView7);

            arrive.setText(cActivity.getString(arriveIndex));
            leave.setText(cActivity.getString(leaveIndex));
            location.setText(cActivity.getString(locationIndex));
            activity.setText(cActivity.getString(activityIndex));

            latitude = cActivity.getString(latitudeIndex);
            longitude = cActivity.getString(longitudeIndex);
            if(!latitude.equals("") || !longitude.equals("")) {

                coordinate.setText(String.format("%.2f", Double.parseDouble(latitude)) + ", " + String.format("%.2f", Double.parseDouble(longitude)));

            }


        }
        else if(fun.equals("UpdateLocated") || fun.equals("CreateLocated")) {
            /* return after user locates the event */
            if(fun.equals("UpdateLocated")) {

                deleteButton.setVisibility(View.VISIBLE);

            }

            TextView arrive = ((TextView) findViewById(R.id.editText4));
            TextView leave = ((TextView) findViewById(R.id.editText12));
            TextView location = ((TextView) findViewById(R.id.editText13));
            TextView activity = ((TextView) findViewById(R.id.editText5));
            TextView coordinate = (TextView) findViewById(R.id.textView7);

            arrive.setText(oldIntent.getStringExtra("arrive"));
            leave.setText(oldIntent.getStringExtra("leave"));
            location.setText(oldIntent.getStringExtra("location"));
            String latitudelocal = oldIntent.getStringExtra("latitude");
            String longitudelocal = oldIntent.getStringExtra("longitude");
            if(!latitudelocal.equals("") || ! longitudelocal.equals("")) {

                latitude = latitudelocal;
                longitude = longitudelocal;
                coordinate.setText(String.format("%.2f", Double.parseDouble(latitude)) + ", " + String.format("%.2f", Double.parseDouble(longitude)));

            }
            activity.setText(oldIntent.getStringExtra("activity"));

        }


    }
}
