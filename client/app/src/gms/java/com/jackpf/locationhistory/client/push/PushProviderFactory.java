package com.jackpf.locationhistory.client.push;

import android.app.Application;

import com.jackpf.locationhistory.client.R;
import com.jackpf.locationhistory.client.client.BeaconClientFactory;
import com.jackpf.locationhistory.client.client.ssl.TrustedCertStorage;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.ui.Toasts;
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;

/**
 * GMS flavour factory — returns a {@link FirebasePushProvider} wired with a
 * {@link PushHandlerRegistrar} that delegates to {@link PushRegistration}.
 * The corresponding FOSS factory lives in app/src/foss/.
 */
public class PushProviderFactory {

    private static final Logger log = new Logger("PushProviderFactory");

    public static PushProvider create(Application application) {
        ConfigRepository config = new ConfigRepository(application);
        PushStorage storage = new PushStorage(application);

        // The registrar is created lazily per-call so that it picks up the
        // latest server connection details at the time of registration.
        PushHandlerRegistrar registrar = new PushHandlerRegistrar() {
            @Override
            public void registerPushHandler(String name, String endpoint) {
                try {
                    BeaconClient client = BeaconClientFactory.createPooledClient(
                            new BeaconClientFactory.BeaconClientParams(
                                    config.getServerHost(),
                                    config.getServerPort(),
                                    false,
                                    BeaconClientFactory.DEFAULT_TIMEOUT
                            ),
                            new TrustedCertStorage(application)
                    );
                    new PushRegistration(application, config, storage, client).register(name, endpoint);
                } catch (IOException e) {
                    log.e(e, "Failed to create beacon client for FCM registration");
                    storage.setEnabled(false);
                    Toasts.show(application, R.string.toast_connection_failed, e.getMessage());
                }
            }

            @Override
            public void unregisterPushHandler() {
                try {
                    BeaconClient client = BeaconClientFactory.createPooledClient(
                            new BeaconClientFactory.BeaconClientParams(
                                    config.getServerHost(),
                                    config.getServerPort(),
                                    false,
                                    BeaconClientFactory.DEFAULT_TIMEOUT
                            ),
                            new TrustedCertStorage(application)
                    );
                    new PushRegistration(application, config, storage, client).unregister();
                } catch (IOException e) {
                    log.e(e, "Failed to create beacon client for FCM unregistration");
                    Toasts.show(application, R.string.toast_connection_failed, e.getMessage());
                }
            }
        };

        return new FirebasePushProvider(application, registrar, storage);
    }
}
