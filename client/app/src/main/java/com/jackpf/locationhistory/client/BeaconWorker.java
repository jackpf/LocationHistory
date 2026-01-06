package com.jackpf.locationhistory.client;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.SetLocationResponse;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.location.LocationProvider;
import com.jackpf.locationhistory.client.model.DeviceState;
import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.service.DeviceStateService;
import com.jackpf.locationhistory.client.service.LocationUpdateService;
import com.jackpf.locationhistory.client.util.Logger;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import lombok.SneakyThrows;

public class BeaconWorker extends ListenableWorker {
    private final ConfigRepository configRepository;
    private final DeviceState deviceState;
    private final LocationProvider locationProvider;
    private final BeaconClient beaconClient;
    private final PermissionsManager permissionsManager;
    private final Executor backgroundExecutor;

    // Services
    private final DeviceStateService deviceStateService;
    private final LocationUpdateService locationUpdateService;

    private static final String WORKER_FUTURE_NAME = "BeaconWorkerFuture";
    public static final String WORKER_DATA_RUN_TIMESTAMP = "BeaconWorkerRunTimestamp";
    public static final String WORKER_DATA_MESSAGE = "BeaconWorkerMessage";

    private final Logger log = new Logger(this);

    @SneakyThrows
    public BeaconWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);

        configRepository = new ConfigRepository(getApplicationContext());
        deviceState = DeviceState.fromConfig(configRepository);
        log.i("Created device state with device ID: %s", deviceState.getDeviceId());

        permissionsManager = new PermissionsManager(getApplicationContext());
        locationProvider = new LocationProvider(getApplicationContext(), permissionsManager);
        beaconClient = BeaconClientFactory.createClient(configRepository);
        backgroundExecutor = Executors.newSingleThreadExecutor();

        deviceStateService = new DeviceStateService(beaconClient, backgroundExecutor);
        locationUpdateService = new LocationUpdateService(beaconClient, backgroundExecutor);
    }

    private Data completeData(String message, boolean updateRunTimestamp) {
        Data.Builder builder = new Data.Builder();
        builder.putString(WORKER_DATA_MESSAGE, message);
        if (updateRunTimestamp)
            builder.putLong(WORKER_DATA_RUN_TIMESTAMP, System.currentTimeMillis());
        return builder.build();
    }

    private void completeNoLocationPermissions(CallbackToFutureAdapter.Completer<Result> completer) {
        String message = "Completing with failure: no location permissions";
        log.e(message);
        finish(completer, Result.failure(completeData(message, false)));
    }

    private void completeDeviceNotReady(CallbackToFutureAdapter.Completer<Result> completer) {
        String message = "Completing with retry: device not ready";
        log.w(message);
        finish(completer, Result.retry());
    }

    private void completeDeviceCheckError(CallbackToFutureAdapter.Completer<Result> completer, Throwable t) {
        String message = "Completing with failure: device check error";
        log.e(t, message);
        finish(completer, Result.failure(completeData(message, false)));
    }

    private void completeEmptyLocationData(CallbackToFutureAdapter.Completer<Result> completer) {
        String message = "Completing with failure: empty location data";
        log.e(message);
        finish(completer, Result.failure(completeData(message, false)));
    }

    private void completeSetLocationSuccess(CallbackToFutureAdapter.Completer<Result> completer) {
        String message = "Completing with success: location updated";
        log.i(message);
        finish(completer, Result.success(completeData(message, true)));
    }

    private void completeSetLocationFailure(CallbackToFutureAdapter.Completer<Result> completer) {
        String message = "Completing with failure: location update failed";
        log.e(message);
        finish(completer, Result.failure(completeData(message, false)));
    }

    private void completeSetLocationError(CallbackToFutureAdapter.Completer<Result> completer, Throwable t) {
        String message = "Completing with failure: set location error";
        log.e(t, message);
        finish(completer, Result.failure(completeData(message, false)));
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        log.d("BeaconWorker doing work...");

        return CallbackToFutureAdapter.getFuture(completer -> {
            backgroundExecutor.execute(() -> {
                if (!permissionsManager.hasLocationPermissions()) {
                    completeNoLocationPermissions(completer);
                    return;
                }

                Futures.addCallback(deviceStateService.onDeviceStateReady(deviceState), new FutureCallback<>() {
                    @Override
                    public void onSuccess(DeviceState state) {
                        if (state.isReady()) handleLocationUpdate(completer);
                        else completeDeviceNotReady(completer);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        completeDeviceCheckError(completer, t);
                    }
                }, backgroundExecutor);
            });

            return WORKER_FUTURE_NAME;
        });
    }

    private void handleLocationUpdate(CallbackToFutureAdapter.Completer<Result> completer) {
        log.d("Updating location");

        locationProvider.getLocation(locationData -> {
            if (locationData != null) {
                log.d("Received location data: %s", locationData.toString());

                Futures.addCallback(locationUpdateService.setLocation(
                        deviceState,
                        locationData
                ), new FutureCallback<>() {
                    @Override
                    public void onSuccess(SetLocationResponse response) {
                        if (response.getSuccess()) {
                            deviceState.setLastRunTimestamp(System.currentTimeMillis());
                            completeSetLocationSuccess(completer);
                        } else completeSetLocationFailure(completer);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        completeSetLocationError(completer, t);
                    }
                }, backgroundExecutor);
            } else completeEmptyLocationData(completer);
        });
    }

    private void finish(CallbackToFutureAdapter.Completer<Result> completer, Result result) {
        try {
            close();
        } finally {
            deviceState.storeToConfig(configRepository);
            completer.set(result);
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        log.d("Worker stopped");
        close();
    }

    private void close() {
        log.d("Closing resources");
        try {
            beaconClient.close();
            locationProvider.close();
        } catch (Exception e) {
            log.e("Error closing resources", e);
        }
    }
}
