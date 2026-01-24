package com.jackpf.locationhistory.client.ui;

import android.app.Application;

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
import com.jackpf.locationhistory.client.location.LocationService;
import com.jackpf.locationhistory.client.push.UnifiedPushContext;
import com.jackpf.locationhistory.client.util.AppExecutors;
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SettingsViewModel extends AndroidViewModel {
    private final Logger log = new Logger(this);

    private final ConfigRepository configRepository;
    private final TrustedCertStorage trustedCertStorage;
    private final UnifiedPushContext unifiedPushContext;
    private final LocationService locationService;

    private final SingleLiveEvent<SettingsViewEvent> events = new SingleLiveEvent<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        this.configRepository = new ConfigRepository(application);
        this.trustedCertStorage = new TrustedCertStorage(application);
        this.unifiedPushContext = new UnifiedPushContext(application);
        this.locationService = LocationService.create(
                application,
                AppExecutors.getInstance().background()
        );
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

    public void saveSettings(String host, String portText, String updateIntervalMinutes) {
        try {
            configRepository.setServerHost(host);
            configRepository.setServerPort(Integer.parseInt(portText));
            configRepository.setUpdateIntervalMillis(TimeUnit.MINUTES.toMillis(Long.parseLong(updateIntervalMinutes)));

            events.postValue(new SettingsViewEvent.Toast(R.string.toast_saved));
        } catch (NumberFormatException e) {
            events.postValue(new SettingsViewEvent.Toast(R.string.toast_invalid_settings, e.getMessage()));
        }
    }

    public void handleUnifiedPushToggle(boolean isChecked) {
        if (isChecked) {
            List<String> distributors = unifiedPushContext.getDistributors();
            log.d("Found distributors: %s", Arrays.toString(distributors.toArray()));

            if (distributors.isEmpty()) {
                events.postValue(new SettingsViewEvent.PromptNtfyInstall());
            } else if (distributors.size() == 1) {
                unifiedPushContext.register(distributors.get(0));
            } else {
                events.postValue(new SettingsViewEvent.ShowDistributorPicker(distributors));
            }
        } else {
            unifiedPushContext.unregister();
        }
    }

    public void registerUnifiedPush(String distributor) {
        log.d("Registering with distributor: %s", distributor);
        unifiedPushContext.register(distributor);
    }

    /**
     * Get the list of location provider items for the settings UI.
     * Providers are returned in priority order based on saved preferences.
     * Providers not in saved preferences are added at the end (disabled by default).
     */
    public List<LocationProviderItem> getLocationProviderItems() {
        List<String> availableProviders = locationService.getAvailableSources();
        List<String> enabledProviders = configRepository.getEnabledLocationProviders();
        Set<String> enabledSet = new HashSet<>(enabledProviders);
        Set<String> availableSet = new HashSet<>(availableProviders);

        List<LocationProviderItem> items = new ArrayList<>();

        // First add enabled providers in their saved order (if still available)
        for (String provider : enabledProviders) {
            if (availableSet.contains(provider)) {
                items.add(new LocationProviderItem(provider, true));
            }
        }

        // Then add remaining available providers (not enabled) at the end
        for (String provider : availableProviders) {
            if (!enabledSet.contains(provider)) {
                items.add(new LocationProviderItem(provider, false));
            }
        }

        return items;
    }

    /**
     * Save the enabled location providers in order.
     */
    public void saveEnabledLocationProviders(List<LocationProviderItem> items) {
        List<String> enabledProviders = items.stream()
                .filter(LocationProviderItem::isEnabled)
                .map(LocationProviderItem::getProviderName)
                .collect(Collectors.toList());

        configRepository.setEnabledLocationProviders(enabledProviders);
        log.d("Saved enabled providers: %s", enabledProviders);
    }
}
