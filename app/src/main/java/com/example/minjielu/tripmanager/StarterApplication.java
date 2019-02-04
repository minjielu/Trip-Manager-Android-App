/* Upon starting the application, a channel for push notification is initialized
   and a notification is scheduled through the alarmManager */

package com.example.minjielu.tripmanager;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;




public class StarterApplication extends Application {

    private SQLiteDatabase tripManager;

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = getString(R.string.app_name);
        String description = "Time to leave";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel("TripManager", name, importance);
        channel.setDescription(description);
        channel.enableLights(true);
        channel.setLightColor(Color.GREEN);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void scheduleNotification() {//delay is after how much time(in millis) from current time you want to schedule the notification
        tripManager = this.openOrCreateDatabase("TripManager", MODE_PRIVATE, null);
        tripManager.execSQL("CREATE TABLE IF NOT EXISTS activities (date TEXT, arrive TEXT, leave TEXT, location TEXT, " +
                "latitude TEXT, longitude TEXT, activity TEXT, tripname TEXT, legal INT(1), number INT(1), FOREIGN KEY(tripname) REFERENCES trips(tripname))");
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
                createNotificationChannel();

                Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
                stackBuilder.addNextIntentWithParentStack(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                /* Build a notification */
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "TripManager")
                        .setContentTitle("Don't be fascinated!")
                        .setContentText("Time to head to " + nextLocation)
                        .setAutoCancel(true)
                        .setContentIntent(resultPendingIntent)
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

    @Override
    public void onCreate() {
        super.onCreate();

        scheduleNotification();


    }
}
