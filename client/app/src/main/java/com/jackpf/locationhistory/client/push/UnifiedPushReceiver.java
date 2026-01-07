package com.jackpf.locationhistory.client.push;

import android.content.Context;

import androidx.annotation.NonNull;

import com.jackpf.locationhistory.client.util.Logger;

import org.unifiedpush.android.connector.FailedReason;
import org.unifiedpush.android.connector.MessagingReceiver;
import org.unifiedpush.android.connector.data.PushEndpoint;
import org.unifiedpush.android.connector.data.PushMessage;

public class UnifiedPushReceiver extends MessagingReceiver {
    private final Logger log = new Logger(this);

    @Override
    public void onNewEndpoint(@NonNull Context context, @NonNull PushEndpoint pushEndpoint, @NonNull String instance) {
        log.i("UnifiedPush: onNewEndpoint: %s", pushEndpoint.getUrl());
        // TODO Register with beacon server
    }

    @Override
    public void onRegistrationFailed(@NonNull Context context, @NonNull FailedReason failedReason, @NonNull String instance) {
        log.e("UnifiedPush: onRegistrationFailed: %s", failedReason.toString());
        // TODO
    }

    @Override
    public void onUnregistered(@NonNull Context context, @NonNull String instance) {
        log.i("UnifiedPush: onUnregistered");
        // TODO
    }

    @Override
    public void onMessage(@NonNull Context context, @NonNull PushMessage pushMessage, @NonNull String instance) {
        String message = new String(pushMessage.getContent());

        log.i("UnifiedPush: onMessage: %s", message);
    }
}
