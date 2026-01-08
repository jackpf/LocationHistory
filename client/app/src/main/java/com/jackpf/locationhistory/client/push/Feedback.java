package com.jackpf.locationhistory.client.push;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class Feedback {
    private static final String CHANNEL_ID = "UnifiedPush";
    private static final String NAME = "Location History";

    private Feedback() {
    }

    public static void createNotification(
            Context context,
            int id,
            String title,
            String text
    ) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        notificationManager.notify(id, notification);
    }

    public static void toast(Context context, int resId, Object... formatArgs) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, context.getString(resId, formatArgs), Toast.LENGTH_SHORT).show());
    }
}
