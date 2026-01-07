package com.jackpf.locationhistory.client.push;

import androidx.annotation.NonNull;

import com.jackpf.locationhistory.client.BeaconWorkerFactory;
import com.jackpf.locationhistory.client.util.Logger;

import org.unifiedpush.android.connector.FailedReason;
import org.unifiedpush.android.connector.PushService;
import org.unifiedpush.android.connector.data.PushEndpoint;
import org.unifiedpush.android.connector.data.PushMessage;

public class UnifiedPushService extends PushService {
    private final Logger log = new Logger(this);

    @Override
    public void onNewEndpoint(@NonNull PushEndpoint pushEndpoint, @NonNull String instance) {
        log.i("UnifiedPush: onNewEndpoint: %s", pushEndpoint.getUrl());
        // TODO Register with beacon server

        UnifiedPushStorage storage = new UnifiedPushStorage(getApplicationContext());
        storage.setEndpoint(pushEndpoint.getUrl());
    }

    @Override
    public void onMessage(@NonNull PushMessage pushMessage, @NonNull String instance) {
        String message = new String(pushMessage.getContent());

        log.i("UnifiedPush: onMessage: %s", message);

        if ("beacon".equals(message)) {
            log.d("Triggering on-demand beacon");
            BeaconWorkerFactory.runOnce(getApplicationContext());
        }
    }

    @Override
    public void onRegistrationFailed(@NonNull FailedReason failedReason, @NonNull String instance) {
        log.e("UnifiedPush: onRegistrationFailed: %s", failedReason.toString());
        // TODO
    }

    @Override
    public void onUnregistered(@NonNull String instance) {
        log.i("UnifiedPush: onUnregistered");
        // TODO
    }
}
