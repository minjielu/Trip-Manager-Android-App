/* This file schedule a push notification for departure when the app is opened */

package com.example.minjielu.tripmanager;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CustomNotificationPublisher extends BroadcastReceiver {



    public void scheduleNotification(Context context) {//delay is after how much time(in millis) from current time you want to schedule the notification
        SQLiteDatabase tripManager = context.openOrCreateDatabase("TripManager", Context.MODE_PRIVATE,null);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH:mm");
        SimpleDateFormat simpleTotalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date curDateTime = new Date();
        String curDate = simpleDateFormat.format(curDateTime);
        String curTime = simpleTimeFormat.format(curDateTime);

        /* Search for the next happening event */
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
            cActivity = tripManager.rawQuery("SELECT rowid, location, tripname, date FROM activities WHERE legal = 1 and ((date = '" + curDate + "' and arrive > '" + curLeave + "') or date > '" + curDate + "') ORDER BY date, arrive LIMIT 1", null);
            cActivity.moveToFirst();
            if(!cActivity.isAfterLast()) {

                int rowId = cActivity.getInt(cActivity.getColumnIndex("rowid"));
                int locationIndex = cActivity.getColumnIndex("location");
                String nextLocation = cActivity.getString(locationIndex);
                Intent resultIntent = new Intent(context, MainActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addNextIntentWithParentStack(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                /* Build the notification */
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "TripManager")
                        .setContentTitle("Don't be fascinated!")
                        .setContentText("Time to head to " + nextLocation)
                        .setAutoCancel(true)
                        .setContentIntent(resultPendingIntent)
                        .setSmallIcon(R.mipmap.ic_launcher);


                Notification notification = builder.build();
                /* Send the notification to the alarmManager */
                Intent notificationIntent = new Intent(context, CustomNotificationPublisher.class);
                notificationIntent.putExtra("notification_id", Integer.toString(rowId + 1000));
                notificationIntent.putExtra("notification", notification);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Calendar cal = Calendar.getInstance();
                cal.setTime(notificationDate);
                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);


            }
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        /* Push the notification when it's received from the alarmManager */
        int nId = Integer.parseInt(intent.getStringExtra("notification_id"));
        Notification notification = intent.getParcelableExtra("notification");
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        scheduleNotification(context);
        notificationManager.notify(nId, notification);
    }
}
