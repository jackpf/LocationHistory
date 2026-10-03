package com.jackpf.locationhistory.client.push;

import static org.unifiedpush.android.connector.ConstantsKt.INSTANCE_DEFAULT;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.util.concurrent.Futures;
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

import org.unifiedpush.android.connector.FailedReason;
import org.unifiedpush.android.connector.PushService;
import org.unifiedpush.android.connector.data.PushEndpoint;
import org.unifiedpush.android.connector.data.PushMessage;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UnifiedPushService extends PushService {
    @Nullable
    private ExecutorService executor;
    @Nullable
    private ConfigRepository configRepository;
    @Nullable
    private PushStorage pushStorage;
    @Nullable
    private MessageHandler messageHandler;
    @Nullable
    private BeaconClient beaconClient;
    @Nullable
    private BeaconScheduler beaconScheduler;

    private final static Logger log = new Logger("UnifiedPushService");

    private static final String NAME = "UnifiedPush";
    static final String CUSTOM_UNREGISTER_ACTION = "com.jackpf.locationhistory.client.MANUAL_UNREGISTER";
    private static final long messageHandlerCooldownMillis = TimeUnit.MILLISECONDS.toMillis(5000);

    private static BeaconClient createBeaconClient(Context context, ConfigRepository configRepository) throws IOException {
        return BeaconClientFactory.createPooledClient(
                new BeaconClientFactory.BeaconClientParams(
                        configRepository.getServerHost(),
                        configRepository.getServerPort(),
                        false,
                        BeaconClientFactory.DEFAULT_TIMEOUT
                ),
                new TrustedCertStorage(context)
        );
    }

    @Override
    public void onCreate() {
        super.onCreate();

        executor = Executors.newSingleThreadExecutor();
        configRepository = new ConfigRepository(getApplicationContext());
        pushStorage = new PushStorage(getApplicationContext());
        messageHandler = new MessageHandler(this, configRepository, executor, messageHandlerCooldownMillis);
        try {
            beaconClient = createBeaconClient(getApplicationContext(), configRepository);
        } catch (IOException e) {
            log.e(e, "Failed to create beacon client for unified push service");
            Toasts.show(getApplicationContext(), R.string.toast_connection_failed, e.getMessage());
        }
        beaconScheduler = BeaconScheduler.create(getApplicationContext(), BeaconScheduler.DEFAULT_WAKELOCK_TIMEOUT);
    }

    /**
     * Manually handle our custom unregister event since UnifiedPush does not
     * trigger onUnregistered automatically when we call unregister().
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent != null && CUSTOM_UNREGISTER_ACTION.equals(intent.getAction())) {
            String instance = intent.getStringExtra("instance");
            onUnregistered(instance != null ? instance : "");
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (executor != null) executor.shutdown();
    }

    @Override
    public void onNewEndpoint(@NonNull PushEndpoint pushEndpoint, @NonNull String instance) {
        log.i("UnifiedPush: onNewEndpoint: %s", pushEndpoint.getUrl());

        if (configRepository != null && beaconClient != null && pushStorage != null) {
            new PushRegistration(getApplicationContext(), configRepository, pushStorage, beaconClient)
                    .register(NAME, pushEndpoint.getUrl());
        } else {
            if (pushStorage != null) pushStorage.setEnabled(false);
        }
    }

    @Override
    public void onMessage(@NonNull PushMessage pushMessage, @NonNull String instance) {
        beaconScheduler.runWithWakeLock(() -> {
            try {
                Notification notification = Notification.parseFrom(pushMessage.getContent());
                log.i("UnifiedPush: onMessage: %s", notification.toString());
                return messageHandler.handle(notification);
            } catch (InvalidProtocolBufferException e) {
                log.e("Failed to parse notification", e);
                return Futures.immediateFailedFuture(e);
            }
        });
    }

    @Override
    public void onRegistrationFailed(@NonNull FailedReason failedReason, @NonNull String instance) {
        log.e("UnifiedPush: onRegistrationFailed: %s", failedReason.toString());

        Toasts.show(getApplicationContext(), R.string.toast_register_push_handler_error, failedReason.toString());
    }

    @Override
    public void onUnregistered(@NonNull String instance) {
        log.i("UnifiedPush: onUnregistered");

        if (configRepository != null && beaconClient != null && pushStorage != null) {
            new PushRegistration(getApplicationContext(), configRepository, pushStorage, beaconClient)
                    .unregister();
        } else {
            if (pushStorage != null) pushStorage.setEnabled(true);
        }
    }
}
