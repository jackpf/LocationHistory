package com.jackpf.locationhistory.client.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

public class Notifications {
    private static final String NOTIFICATIONS_CHANNEL = "notifications";
    private static final String ALARM_CHANNEL = "alarms";
    private static final String NAME = "Location History";

    private final Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    private final AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build();

    private final Context context;
    private final NotificationManager notificationManager;

    public Notifications(Context context) {
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    NOTIFICATIONS_CHANNEL,
                    NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(notificationChannel);

            NotificationChannel alarmChannel = new NotificationChannel(
                    ALARM_CHANNEL,
                    NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            alarmChannel.setSound(alarmUri, audioAttributes);
            alarmChannel.enableVibration(true);
            notificationManager.createNotificationChannel(alarmChannel);
        }
    }

    public void createNotification(
            Context context,
            int id,
            String title,
            String text
    ) {
        Notification notification = new NotificationCompat.Builder(context, NOTIFICATIONS_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        notificationManager.notify(id, notification);
    }

    public void triggerAlarm() {
        Notification notification = new NotificationCompat.Builder(context, ALARM_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Alarm Triggered")
                .setContentText("Alarm triggered via Location History.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setSound(alarmUri)
                .build();

        notificationManager.notify(0, notification);
    }
}
