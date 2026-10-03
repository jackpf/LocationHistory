package com.jackpf.locationhistory.client.push;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.jackpf.locationhistory.Notification;
import com.jackpf.locationhistory.client.BeaconScheduler;
import com.jackpf.locationhistory.client.R;
import com.jackpf.locationhistory.client.client.BeaconClientFactory;
import com.jackpf.locationhistory.client.client.ssl.TrustedCertStorage;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.ui.Toasts;
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * GMS flavour push service using Firebase Cloud Messaging.
 * Mirrors UnifiedPushService in the foss source set.
 *
 * FCM data messages must include a "payload" key containing the Base64-encoded
 * serialised Notification protobuf.
 */
public class FirebasePushService extends FirebaseMessagingService {

    private static final String NAME = "FCM";
    private static final String PAYLOAD_KEY = "payload";
    private static final long MESSAGE_HANDLER_COOLDOWN_MILLIS = TimeUnit.MILLISECONDS.toMillis(5000);

    private final Logger log = new Logger("FirebasePushService");

    private ExecutorService executor;
    private ConfigRepository configRepository;
    private PushStorage pushStorage;
    private MessageHandler messageHandler;
    private BeaconScheduler beaconScheduler;

    @Override
    public void onCreate() {
        super.onCreate();

        executor = Executors.newSingleThreadExecutor();
        configRepository = new ConfigRepository(getApplicationContext());
        pushStorage = new PushStorage(getApplicationContext());
        messageHandler = new MessageHandler(this, configRepository, executor, MESSAGE_HANDLER_COOLDOWN_MILLIS);
        beaconScheduler = BeaconScheduler.create(getApplicationContext(), BeaconScheduler.DEFAULT_WAKELOCK_TIMEOUT);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
    }

    /**
     * Called by Firebase when a new token is issued (initial registration or rotation).
     * Re-registers with the server using the new token.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        log.i("FCM: onNewToken");

        try {
            BeaconClient beaconClient = BeaconClientFactory.createPooledClient(
                    new BeaconClientFactory.BeaconClientParams(
                            configRepository.getServerHost(),
                            configRepository.getServerPort(),
                            false,
                            BeaconClientFactory.DEFAULT_TIMEOUT
                    ),
                    new TrustedCertStorage(getApplicationContext())
            );
            new PushRegistration(getApplicationContext(), configRepository, pushStorage, beaconClient)
                    .register(NAME, token);
        } catch (IOException e) {
            log.e(e, "Failed to create beacon client for FCM token registration");
            Toasts.show(getApplicationContext(), R.string.toast_connection_failed, e.getMessage());
        }
    }

    /**
     * Called when a data message arrives from FCM.
     * Expects a "payload" key with a Base64-encoded serialised Notification proto.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String encodedPayload = remoteMessage.getData().get(PAYLOAD_KEY);
        if (encodedPayload == null) {
            log.w("FCM: received message without '%s' key — ignoring", PAYLOAD_KEY);
            return;
        }

        byte[] rawBytes = Base64.getDecoder().decode(encodedPayload);

        beaconScheduler.runWithWakeLock(() -> {
            try {
                Notification notification = Notification.parseFrom(rawBytes);
                log.i("FCM: onMessageReceived: %s", notification.toString());
                return messageHandler.handle(notification);
            } catch (InvalidProtocolBufferException e) {
                log.e("Failed to parse FCM notification", e);
                return com.google.common.util.concurrent.Futures.immediateFailedFuture(e);
            }
        });
    }
}
