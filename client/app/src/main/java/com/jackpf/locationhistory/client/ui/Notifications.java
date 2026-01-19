package com.jackpf.locationhistory.client.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

import com.jackpf.locationhistory.client.R;

public class Notifications {
    private static final String NOTIFICATIONS_CHANNEL = "notifications";
    private static final String PERSISTENT_NOTIFICATIONS_CHANNEL = "persistent_notifications";
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
            // Notification channel
            NotificationChannel notificationChannel = new NotificationChannel(
                    NOTIFICATIONS_CHANNEL,
                    NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(notificationChannel);

            // Persistent notification channel
            NotificationChannel persistentNotificationChannel = new NotificationChannel(
                    PERSISTENT_NOTIFICATIONS_CHANNEL,
                    NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(persistentNotificationChannel);

            // Alarm channel
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

    public Notification createNotification(
            String title,
            String text
    ) {
        return new NotificationCompat.Builder(context, NOTIFICATIONS_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    public Notification createPersistentNotification(
            String title,
            String text
    ) {
        return new NotificationCompat.Builder(context, PERSISTENT_NOTIFICATIONS_CHANNEL)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
    }

    public Notification createAlarmNotification() {
        return new NotificationCompat.Builder(context, ALARM_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle(context.getString(R.string.notification_alarm_title))
                .setContentText(context.getString(R.string.notification_alarm_description))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setSound(alarmUri)
                .build();
    }

    public void show(int id, Notification notification) {
        notificationManager.notify(id, notification);
    }
}
