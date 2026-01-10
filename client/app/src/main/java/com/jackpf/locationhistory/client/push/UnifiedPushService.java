package com.jackpf.locationhistory.client.push;

import static org.unifiedpush.android.connector.ConstantsKt.INSTANCE_DEFAULT;

import android.content.Context;

import androidx.annotation.NonNull;

import com.jackpf.locationhistory.client.BeaconWorkerFactory;
import com.jackpf.locationhistory.client.R;
import com.jackpf.locationhistory.client.ui.Notifications;
import com.jackpf.locationhistory.client.ui.Toasts;
import com.jackpf.locationhistory.client.util.Logger;

import org.unifiedpush.android.connector.FailedReason;
import org.unifiedpush.android.connector.PushService;
import org.unifiedpush.android.connector.UnifiedPush;
import org.unifiedpush.android.connector.data.PushEndpoint;
import org.unifiedpush.android.connector.data.PushMessage;

public class UnifiedPushService extends PushService {
    private static final String NAME = "UnifiedPush";
    private static final String BEACON_MESSAGE = "TRIGGER_BEACON";
    private static final String ALARM_MESSAGE = "TRIGGER_ALARM";

    private final Logger log = new Logger(this);

    @Override
    public void onNewEndpoint(@NonNull PushEndpoint pushEndpoint, @NonNull String instance) {
        log.i("UnifiedPush: onNewEndpoint: %s", pushEndpoint.getUrl());

        new PushRegistration(getApplicationContext()).register(NAME, pushEndpoint.getUrl());
    }

    @Override
    public void onMessage(@NonNull PushMessage pushMessage, @NonNull String instance) {
        String message = new String(pushMessage.getContent());
        log.i("UnifiedPush: onMessage: %s", message);

        if (BEACON_MESSAGE.equals(message)) {
            log.d("Triggering on-demand beacon");
            BeaconWorkerFactory.runOnce(getApplicationContext());
        } else if (ALARM_MESSAGE.equals(message)) {
            log.d("Triggering on-demand alarm");
            new Notifications(getApplicationContext()).triggerAlarm();
        }
    }

    @Override
    public void onRegistrationFailed(@NonNull FailedReason failedReason, @NonNull String instance) {
        log.e("UnifiedPush: onRegistrationFailed: %s", failedReason.toString());

        Toasts.show(getApplicationContext(), R.string.toast_register_push_handler_error, failedReason.toString());
    }

    @Override
    public void onUnregistered(@NonNull String instance) {
        log.i("UnifiedPush: onUnregistered");

        new PushRegistration(getApplicationContext()).unregister();
    }

    public static void register(Context context, String distributor) {
        UnifiedPush.saveDistributor(context, distributor);

        UnifiedPush.register(
                context,
                INSTANCE_DEFAULT,
                "",
                null
        );
    }

    public static void unregister(Context context) {
        UnifiedPush.unregister(
                context,
                INSTANCE_DEFAULT
        );

        new PushRegistration(context).unregister();
    }
}
