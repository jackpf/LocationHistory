package com.jackpf.locationhistory.client.ui;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.jackpf.locationhistory.PingResponse;
import com.jackpf.locationhistory.client.R;
import com.jackpf.locationhistory.client.client.BeaconClientFactory;
import com.jackpf.locationhistory.client.client.ssl.TrustedCertStorage;
import com.jackpf.locationhistory.client.client.ssl.UntrustedCertException;
import com.jackpf.locationhistory.client.client.util.GrpcFutureWrapper;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.push.Ntfy;
import com.jackpf.locationhistory.client.push.UnifiedPushService;
import com.jackpf.locationhistory.client.util.Logger;

import org.unifiedpush.android.connector.UnifiedPush;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SettingsViewModel extends AndroidViewModel {
    private final Logger log = new Logger(this);

    private final ConfigRepository configRepository;
    private final TrustedCertStorage trustedCertStorage;

    private final SingleLiveEvent<SettingsViewEvent> events = new SingleLiveEvent<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        this.configRepository = new ConfigRepository(application);
        this.trustedCertStorage = new TrustedCertStorage(application);
    }

    public LiveData<SettingsViewEvent> getEvents() {
        return events;
    }

    public ConfigRepository getConfig() {
        return configRepository;
    }

    public void testConnection(String host, String portText) {
        try {
            BeaconClient tempClient = BeaconClientFactory.createClient(
                    new BeaconClientFactory.BeaconClientParams(host, Integer.parseInt(portText), false, 3000),
                    trustedCertStorage
            );

            ListenableFuture<PingResponse> pingResponse = tempClient.ping(new GrpcFutureWrapper<>(
                    response -> {
                        if (BeaconClient.isPongResponse(response)) {
                            events.postValue(new SettingsViewEvent.Toast(R.string.toast_connection_successful));
                        } else {
                            events.postValue(new SettingsViewEvent.Toast(R.string.toast_invalid_response, response.getMessage()));
                        }
                    },
                    e -> {
                        if (UntrustedCertException.isCauseOf(e)) {
                            String fingerprint = UntrustedCertException.getCauseFrom(e).getFingerprint();
                            events.postValue(new SettingsViewEvent.SslPrompt(fingerprint));
                        } else {
                            events.postValue(new SettingsViewEvent.Toast(R.string.toast_connection_failed, e.getMessage()));
                        }
                    }
            ));

            pingResponse.addListener(tempClient::close, MoreExecutors.directExecutor());
        } catch (NumberFormatException | IOException e) {
            events.postValue(new SettingsViewEvent.Toast(R.string.toast_invalid_settings, e.getMessage()));
        }
    }

    public void saveSettings(String host, String portText, String updateInterval) {
        try {
            configRepository.setServerHost(host);
            configRepository.setServerPort(Integer.parseInt(portText));
            configRepository.setUpdateIntervalMinutes(Integer.parseInt(updateInterval));
        } catch (NumberFormatException e) {
            events.postValue(new SettingsViewEvent.Toast(R.string.toast_invalid_settings, e.getMessage()));
        }
    }

    public void handleUnifiedPushToggle(Context context, boolean isChecked) {
        if (isChecked) {
            List<String> distributors = UnifiedPush.getDistributors(context);
            log.d("Found distributors: %s", Arrays.toString(distributors.toArray()));

            if (distributors.isEmpty()) {
                Ntfy.promptInstall(context);
                events.postValue(new SettingsViewEvent.SetUnifiedPushChecked(false));
            } else if (distributors.size() == 1) {
                registerUnifiedPush(context, distributors.get(0));
            } else {
                events.postValue(new SettingsViewEvent.ShowDistributorPicker(distributors));
            }
        } else {
            UnifiedPushService.unregister(context);
        }
    }

    public void registerUnifiedPush(Context context, String distributor) {
        log.d("Registering with distributor: %s", distributor);
        UnifiedPushService.register(context, distributor);
    }
}
