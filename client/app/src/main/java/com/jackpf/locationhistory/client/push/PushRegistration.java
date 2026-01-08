package com.jackpf.locationhistory.client.push;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.RegisterPushHandlerResponse;
import com.jackpf.locationhistory.client.BeaconClientFactory;
import com.jackpf.locationhistory.client.R;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.grpc.util.GrpcFutureWrapper;
import com.jackpf.locationhistory.client.ssl.TrustedCertStorage;
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;

public class PushRegistration {
    private final Context context;

    private final UnifiedPushStorage storage;
    private final ConfigRepository configRepository;

    private final Logger log = new Logger(this);

    public PushRegistration(Context context) {
        this.context = context;
        this.storage = new UnifiedPushStorage(context);
        this.configRepository = new ConfigRepository(context);
    }

    private BeaconClient createBeaconClient(ConfigRepository configRepository) throws IOException {
        return BeaconClientFactory.createClient(
                configRepository,
                false,
                new TrustedCertStorage(context)
        );
    }

    public void register(String name, String url) {
        storage.setEndpoint(url);

        try {
            BeaconClient beaconClient = createBeaconClient(configRepository);

            ListenableFuture<RegisterPushHandlerResponse> registerResult = beaconClient.registerPushHandler(
                    configRepository.getDeviceId(),
                    name,
                    url,
                    new GrpcFutureWrapper<>(
                            value -> {
                                if (value.getSuccess()) {
                                    storage.setEnabled(true);
                                    log.d("Registered push handler");
                                    Feedback.toast(context, R.string.toast_register_push_handler_success);
                                } else {
                                    log.e("Registering push handler failed");
                                    Feedback.toast(context, R.string.toast_register_push_handler_failed);
                                }
                            },
                            error -> {
                                log.e(error, "Error registering push handler");
                                Feedback.toast(context, R.string.toast_register_push_handler_error, error.getMessage());
                            }
                    )
            );

            registerResult.addListener(beaconClient::close, ContextCompat.getMainExecutor(context));
        } catch (IOException e) {
            log.e(e, "Failed to create beacon client for push handler registration");
            Feedback.toast(context, R.string.toast_connection_failed, e.getMessage());
        }
    }


    public void unregister() {
        storage.setEndpoint(null);

        try {
            BeaconClient beaconClient = createBeaconClient(configRepository);

            ListenableFuture<RegisterPushHandlerResponse> registerResult = beaconClient.unregisterPushHandler(
                    configRepository.getDeviceId(),
                    new GrpcFutureWrapper<>(
                            value -> {
                                if (value.getSuccess()) {
                                    storage.setEnabled(false);
                                    log.d("Un-registered push handler");
                                    Feedback.toast(context, R.string.toast_unregister_push_handler_success);
                                } else {
                                    log.e("Un-registering push handler failed");
                                    Feedback.toast(context, R.string.toast_unregister_push_handler_failed);
                                }
                            },
                            error -> {
                                log.e(error, "Error un-registering push handler");
                                Feedback.toast(context, R.string.toast_unregister_push_handler_error, error.getMessage());
                            }
                    )
            );

            registerResult.addListener(beaconClient::close, ContextCompat.getMainExecutor(context));
        } catch (IOException e) {
            log.e(e, "Failed to create beacon client for push handler registration");
            Feedback.toast(context, R.string.toast_connection_failed, e.getMessage());
        }
    }
}
