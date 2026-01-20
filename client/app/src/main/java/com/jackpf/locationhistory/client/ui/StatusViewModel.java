package com.jackpf.locationhistory.client.ui;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
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
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StatusViewModel extends AndroidViewModel {
    private final Logger log = new Logger(this);

    private final ConfigRepository configRepository;
    private final TrustedCertStorage trustedCertStorage;

    private final SingleLiveEvent<StatusViewEvent> events = new SingleLiveEvent<>();

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener =
            (sharedPreferences, key) -> updateDeviceState();

    public StatusViewModel(@NonNull Application application) {
        super(application);
        this.configRepository = new ConfigRepository(application);
        this.trustedCertStorage = new TrustedCertStorage(application);
    }

    public LiveData<StatusViewEvent> getEvents() {
        return events;
    }

    public void startListening() {
        configRepository.registerOnSharedPreferenceChangeListener(preferenceListener);
        updateDeviceState();
    }

    public void stopListening() {
        configRepository.unregisterOnSharedPreferenceChangeListener(preferenceListener);
    }

    public ListenableFuture<PingResponse> checkConnection() {
        ListenableFuture<PingResponse> future = testConnection();

        Futures.addCallback(future, new FutureCallback<PingResponse>() {
            @Override
            public void onSuccess(PingResponse result) {
                if (BeaconClient.isPongResponse(result)) {
                    events.postValue(new StatusViewEvent.UpdateConnectionStatus(R.string.connected));
                } else {
                    events.postValue(new StatusViewEvent.UpdateConnectionStatus(R.string.invalid_response));
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                if (UntrustedCertException.isCauseOf(t)) {
                    String fingerprint = UntrustedCertException.getCauseFrom(t).getFingerprint();
                    events.postValue(new StatusViewEvent.ShowSslPrompt(fingerprint));
                } else {
                    events.postValue(new StatusViewEvent.UpdateConnectionStatus(R.string.disconnected));
                }
            }
        }, MoreExecutors.directExecutor());

        return future;
    }

    private ListenableFuture<PingResponse> testConnection() {
        try {
            BeaconClient client = BeaconClientFactory.createPooledClient(
                    new BeaconClientFactory.BeaconClientParams(
                            configRepository.getServerHost(),
                            configRepository.getServerPort(),
                            false,
                            BeaconClientFactory.DEFAULT_TIMEOUT
                    ),
                    trustedCertStorage
            );
            return client.ping(GrpcFutureWrapper.empty());
        } catch (IOException e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    private void updateDeviceState() {
        log.d("Updating device state");

        String deviceState = configRepository.getDeviceStatus();

        long lastRunTimestamp = configRepository.getLastRunTimestamp();
        String lastPing;
        if (lastRunTimestamp > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            lastPing = sdf.format(new Date(lastRunTimestamp));
        } else {
            lastPing = getApplication().getString(R.string.never);
        }

        events.postValue(new StatusViewEvent.UpdateDeviceState(deviceState, lastPing));
    }
}
