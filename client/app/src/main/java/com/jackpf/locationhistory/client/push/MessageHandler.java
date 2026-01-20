package com.jackpf.locationhistory.client.push;

import android.content.Context;

import com.jackpf.locationhistory.AccuracyNotification;
import com.jackpf.locationhistory.AlarmNotification;
import com.jackpf.locationhistory.LocationAccuracyRequest;
import com.jackpf.locationhistory.LocationNotification;
import com.jackpf.locationhistory.Notification;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.ui.Notifications;
import com.jackpf.locationhistory.client.util.Logger;
import com.jackpf.locationhistory.client.worker.BeaconTask;

import java.util.concurrent.Executor;

public class MessageHandler {
    private final Logger log = new Logger(this);

    private final Context context;
    private final ConfigRepository configRepository;
    private final Executor executor;

    private static final int ALARM_NOTIFICATION_ID = 1;

    public MessageHandler(Context context, ConfigRepository configRepository, Executor executor) {
        this.context = context;
        this.configRepository = configRepository;
        this.executor = executor;
    }

    private void updateRequestedAccuracy(LocationAccuracyRequest requestedAccuracy) {
        long triggeredAt = requestedAccuracy == LocationAccuracyRequest.HIGH ? System.currentTimeMillis() : -1;
        configRepository.setHighAccuracyTriggeredAt(triggeredAt);
    }

    private void handleTriggerLocation(LocationNotification notification) {
        log.d("Triggering on-demand beacon");
        if (notification.getRequestAccuracy() == LocationAccuracyRequest.HIGH) {
            // We only "upgrade" balanced requests -> high accuracy if set
            // Otherwise could overwrite/ignore the current high accuracy mode
            updateRequestedAccuracy(notification.getRequestAccuracy());
        }
        BeaconTask.runSafe(context, executor);
    }

    private void handleTriggerAlarm(AlarmNotification notification) {
        log.d("Triggering on-demand alarm");
        Notifications notifications = new Notifications(context);
        notifications.show(ALARM_NOTIFICATION_ID, notifications.createAlarmNotification());
    }

    private void handleSetAccuracy(AccuracyNotification notification) {
        LocationAccuracyRequest requestedAccuracy = notification.getRequestAccuracy();
        log.d("Setting accuracy to %s", requestedAccuracy);
        updateRequestedAccuracy(requestedAccuracy);
    }

    public void handle(Notification notification) {
        log.i("UnifiedPush: onMessage: %s", notification.toString());

        if (notification.hasTriggerLocation()) {
            handleTriggerLocation(notification.getTriggerLocation());
        } else if (notification.hasTriggerAlarm()) {
            handleTriggerAlarm(notification.getTriggerAlarm());
        } else if (notification.hasSetAccuracy()) {
            handleSetAccuracy(notification.getSetAccuracy());
        } else {
            log.w("Received unhandled notification: %s", notification);
        }
    }
}
