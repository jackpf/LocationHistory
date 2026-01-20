package com.jackpf.locationhistory.client.push;

import android.content.Context;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.AccuracyNotification;
import com.jackpf.locationhistory.AlarmNotification;
import com.jackpf.locationhistory.LocationAccuracyRequest;
import com.jackpf.locationhistory.LocationNotification;
import com.jackpf.locationhistory.Notification;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.ui.Notifications;
import com.jackpf.locationhistory.client.util.Logger;
import com.jackpf.locationhistory.client.worker.BeaconResult;
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

    private ListenableFuture<BeaconResult> handleTriggerLocation(LocationNotification notification) {
        log.d("Triggering on-demand beacon");
        if (notification.getRequestAccuracy() == LocationAccuracyRequest.HIGH) {
            // We only "upgrade" balanced requests -> high accuracy if set
            // Otherwise could overwrite/ignore the current high accuracy mode
            updateRequestedAccuracy(notification.getRequestAccuracy());
        }
        return BeaconTask.runSafe(context, executor);
    }

    private ListenableFuture<Void> handleTriggerAlarm(AlarmNotification notification) {
        log.d("Triggering on-demand alarm");
        Notifications notifications = new Notifications(context);
        notifications.show(ALARM_NOTIFICATION_ID, notifications.createAlarmNotification());
        return Futures.immediateVoidFuture();
    }

    private ListenableFuture<Void> handleSetAccuracy(AccuracyNotification notification) {
        LocationAccuracyRequest requestedAccuracy = notification.getRequestAccuracy();
        log.d("Setting accuracy to %s", requestedAccuracy);
        updateRequestedAccuracy(requestedAccuracy);
        return Futures.immediateVoidFuture();
    }

    public ListenableFuture<?> handle(Notification notification) {
        if (notification.hasTriggerLocation()) {
            return handleTriggerLocation(notification.getTriggerLocation());
        } else if (notification.hasTriggerAlarm()) {
            return handleTriggerAlarm(notification.getTriggerAlarm());
        } else if (notification.hasSetAccuracy()) {
            return handleSetAccuracy(notification.getSetAccuracy());
        } else {
            String msg = String.format("Received unhandled notification: %s", notification);
            log.w(msg);
            return Futures.immediateFailedFuture(new UnsupportedOperationException(msg));
        }
    }
}
