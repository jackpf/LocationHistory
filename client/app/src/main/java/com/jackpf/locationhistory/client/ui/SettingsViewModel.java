package com.jackpf.locationhistory.client.ui;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.jackpf.locationhistory.PingResponse;
import com.jackpf.locationhistory.client.BeaconClientFactory;
import com.jackpf.locationhistory.client.BeaconWorkerFactory;
import com.jackpf.locationhistory.client.R;
import com.jackpf.locationhistory.client.client.ssl.TrustedCertStorage;
import com.jackpf.locationhistory.client.client.ssl.UntrustedCertException;
import com.jackpf.locationhistory.client.client.util.GrpcFutureWrapper;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.push.Ntfy;
import com.jackpf.locationhistory.client.push.UnifiedPushService;
import com.jackpf.locationhistory.client.util.Logger;
import com.jackpf.locationhistory.client.util.PermissionException;

import org.unifiedpush.android.connector.UnifiedPush;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SettingsViewModel extends AndroidViewModel {
    private final Logger log = new Logger(this);

    private final ConfigRepository configRepository;
    private final TrustedCertStorage trustedCertStorage;

    private final MutableLiveData<SettingsViewEvent> events = new MutableLiveData<>();

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
            int port = Integer.parseInt(portText);

            BeaconClient tempClient = BeaconClientFactory.createClient(
                    new BeaconClientFactory.BeaconClientParams(host, port, false, 3000),
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

    public void saveSettings(String host, String portText, ConfigRepository.UpdateFrequency frequency, String intervalText) {
        try {
            int port = Integer.parseInt(portText);

            configRepository.setServerHost(host);
            configRepository.setServerPort(port);
            configRepository.setUpdateFrequency(frequency);

            // Save interval if scheduled frequency
            if (frequency == ConfigRepository.UpdateFrequency.SCHEDULED && !intervalText.isEmpty()) {
                configRepository.setUpdateIntervalMinutes(Integer.parseInt(intervalText));
            }

            // Run once and reschedule
            BeaconWorkerFactory.runOnce(getApplication());

            try {
                BeaconWorkerFactory.schedule(getApplication(), configRepository);
                events.postValue(new SettingsViewEvent.Toast(R.string.toast_saved));
            } catch (PermissionException e) {
                log.e("Unable to schedule beacon worker", e);
                events.postValue(new SettingsViewEvent.Toast(R.string.schedule_error));
            }
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
