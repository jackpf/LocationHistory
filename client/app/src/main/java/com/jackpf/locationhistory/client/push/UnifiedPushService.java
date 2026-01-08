package com.jackpf.locationhistory.client.push;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.RegisterPushHandlerResponse;
import com.jackpf.locationhistory.client.BeaconClientFactory;
import com.jackpf.locationhistory.client.BeaconWorkerFactory;
import com.jackpf.locationhistory.client.R;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.grpc.util.GrpcFutureWrapper;
import com.jackpf.locationhistory.client.ssl.TrustedCertStorage;
import com.jackpf.locationhistory.client.util.Logger;

import org.unifiedpush.android.connector.FailedReason;
import org.unifiedpush.android.connector.PushService;
import org.unifiedpush.android.connector.UnifiedPush;
import org.unifiedpush.android.connector.data.PushEndpoint;
import org.unifiedpush.android.connector.data.PushMessage;

import java.io.IOException;

public class UnifiedPushService extends PushService {
    private static final String NAME = "UnifiedPush";
    private static final String BEACON_MESSAGE = "TRIGGER_BEACON";

    private final Logger log = new Logger(this);

    private BeaconClient createBeaconClient(ConfigRepository configRepository) throws IOException {
        return BeaconClientFactory.createClient(
                configRepository,
                false,
                new TrustedCertStorage(getApplicationContext())
        );
    }

    @Override
    public void onNewEndpoint(@NonNull PushEndpoint pushEndpoint, @NonNull String instance) {
        log.i("UnifiedPush: onNewEndpoint: %s", pushEndpoint.getUrl());

        UnifiedPushStorage storage = new UnifiedPushStorage(getApplicationContext());
        storage.setEndpoint(pushEndpoint.getUrl());

        ConfigRepository configRepository = new ConfigRepository(getApplicationContext());

        try {
            BeaconClient beaconClient = createBeaconClient(configRepository);

            ListenableFuture<RegisterPushHandlerResponse> registerResult = beaconClient.registerPushHandler(
                    configRepository.getDeviceId(),
                    NAME,
                    pushEndpoint.getUrl(),
                    new GrpcFutureWrapper<>(
                            value -> {
                                if (value.getSuccess()) {
                                    log.d("Registered push handler");
                                    Feedback.toast(getApplicationContext(), R.string.toast_register_push_handler_success);
                                } else {
                                    log.e("Registering push handler failed");
                                    Feedback.toast(getApplicationContext(), R.string.toast_register_push_handler_failed);
                                }
                            },
                            error -> {
                                log.e(error, "Error registering push handler");
                                Feedback.toast(getApplicationContext(), R.string.toast_register_push_handler_error, error.getMessage());
                            }
                    )
            );

            registerResult.addListener(beaconClient::close, getMainExecutor());
        } catch (IOException e) {
            log.e(e, "Failed to create beacon client for push handler registration");
            Feedback.toast(getApplicationContext(), R.string.toast_connection_failed, e.getMessage());
        }
    }

    @Override
    public void onMessage(@NonNull PushMessage pushMessage, @NonNull String instance) {
        String message = new String(pushMessage.getContent());
        log.i("UnifiedPush: onMessage: %s", message);

        if (BEACON_MESSAGE.equals(message)) {
            log.d("Triggering on-demand beacon");
            BeaconWorkerFactory.runOnce(getApplicationContext());
        }
    }

    @Override
    public void onRegistrationFailed(@NonNull FailedReason failedReason, @NonNull String instance) {
        log.e("UnifiedPush: onRegistrationFailed: %s", failedReason.toString());
    }

    @Override
    public void onUnregistered(@NonNull String instance) {
        log.i("UnifiedPush: onUnregistered");

        UnifiedPushStorage storage = new UnifiedPushStorage(getApplicationContext());
        storage.setEndpoint(null);

        ConfigRepository configRepository = new ConfigRepository(getApplicationContext());

        try {
            BeaconClient beaconClient = createBeaconClient(configRepository);

            ListenableFuture<RegisterPushHandlerResponse> registerResult = beaconClient.unregisterPushHandler(
                    configRepository.getDeviceId(),
                    new GrpcFutureWrapper<>(
                            value -> {
                                if (value.getSuccess()) {
                                    // TODO Strings
                                    log.d("Un-registered push handler");
                                    Feedback.toast(getApplicationContext(), R.string.toast_unregister_push_handler_success);
                                } else {
                                    log.e("Un-registering push handler failed");
                                    Feedback.toast(getApplicationContext(), R.string.toast_unregister_push_handler_failed);
                                }
                            },
                            error -> {
                                log.e(error, "Error un-registering push handler");
                                Feedback.toast(getApplicationContext(), R.string.toast_unregister_push_handler_error, error.getMessage());
                            }
                    )
            );

            registerResult.addListener(beaconClient::close, getMainExecutor());
        } catch (IOException e) {
            log.e(e, "Failed to create beacon client for push handler registration");
            Feedback.toast(getApplicationContext(), R.string.toast_connection_failed, e.getMessage());
        }
    }

    public static void register(Context context, String distributor) {
        UnifiedPush.saveDistributor(context, distributor);

        UnifiedPush.register(
                context,
                "default",
                "",
                null
        );
    }
}
