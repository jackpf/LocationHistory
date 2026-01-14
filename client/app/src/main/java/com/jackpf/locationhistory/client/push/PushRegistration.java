package com.jackpf.locationhistory.client.push;

import android.content.Context;

import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.RegisterPushHandlerResponse;
import com.jackpf.locationhistory.client.R;
import com.jackpf.locationhistory.client.client.util.GrpcFutureWrapper;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.ui.Toasts;
import com.jackpf.locationhistory.client.util.Logger;

public class PushRegistration {
    private final Context context;

    private final UnifiedPushStorage storage;
    private final ConfigRepository configRepository;
    private final BeaconClient beaconClient;

    private final Logger log = new Logger(this);

    public PushRegistration(
            Context context,
            ConfigRepository configRepository,
            UnifiedPushStorage unifiedPushStorage,
            BeaconClient beaconClient
    ) {
        this.context = context;
        this.storage = unifiedPushStorage;
        this.configRepository = configRepository;
        this.beaconClient = beaconClient;
    }

    public ListenableFuture<RegisterPushHandlerResponse> register(String name, String url) {
        storage.setEndpoint(url);

        return beaconClient.registerPushHandler(
                configRepository.getDeviceId(),
                name,
                url,
                new GrpcFutureWrapper<>(
                        value -> {
                            if (value.getSuccess()) {
                                storage.setEnabled(true);
                                log.d("Registered push handler");
                                Toasts.show(context, R.string.toast_register_push_handler_success);
                            } else {
                                log.e("Registering push handler failed");
                                Toasts.show(context, R.string.toast_register_push_handler_failed);
                            }
                        },
                        error -> {
                            log.e(error, "Error registering push handler");
                            Toasts.show(context, R.string.toast_register_push_handler_error, error.getMessage());
                        }
                )
        );
    }

    public ListenableFuture<RegisterPushHandlerResponse> unregister() {
        storage.setEndpoint(null);

        return beaconClient.unregisterPushHandler(
                configRepository.getDeviceId(),
                new GrpcFutureWrapper<>(
                        value -> {
                            if (value.getSuccess()) {
                                storage.setEnabled(false);
                                log.d("Un-registered push handler");
                                Toasts.show(context, R.string.toast_unregister_push_handler_success);
                            } else {
                                log.e("Un-registering push handler failed");
                                Toasts.show(context, R.string.toast_unregister_push_handler_failed);
                            }
                        },
                        error -> {
                            log.e(error, "Error un-registering push handler");
                            Toasts.show(context, R.string.toast_unregister_push_handler_error, error.getMessage());
                        }
                )
        );
    }
}
