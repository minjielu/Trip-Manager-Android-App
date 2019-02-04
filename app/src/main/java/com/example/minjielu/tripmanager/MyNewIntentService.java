/* This file handles the intent containing a notification received from the alarmManager
  , even when the app is shut down */

package com.example.minjielu.tripmanager;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class MyNewIntentService extends JobIntentService {
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        /* Push the notification after a intent containing a notification is received from the alarmManager */
        int nId = Integer.parseInt(intent.getStringExtra("notification_id"));
        Notification notification = intent.getParcelableExtra("notification");
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(nId, notification);
    }
}
