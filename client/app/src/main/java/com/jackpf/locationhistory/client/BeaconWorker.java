package com.jackpf.locationhistory.client;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
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

    private final String WORKER_FUTURE_NAME = "BeaconWorkerFuture";

    private final Logger log = new Logger(this);

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

    private void completeNoLocationPermissions(CallbackToFutureAdapter.Completer<Result> completer) {
        log.e("Completing with failure: no location permissions");
        finish(completer, Result.failure());
    }

    private void completeDeviceNotReady(CallbackToFutureAdapter.Completer<Result> completer) {
        log.w("Completing with retry: device not ready");
        finish(completer, Result.retry());
    }

    private void completeDeviceCheckError(CallbackToFutureAdapter.Completer<Result> completer, Throwable t) {
        log.e(t, "Completing with failure: device not ready");
        finish(completer, Result.failure());
    }

    private void completeEmptyLocationData(CallbackToFutureAdapter.Completer<Result> completer) {
        log.e("Completing with failure: empty location data");
        finish(completer, Result.failure());
    }

    private void completeSetLocationSuccess(CallbackToFutureAdapter.Completer<Result> completer) {
        log.i("Completing with success: location updated");
        finish(completer, Result.success());
    }

    private void completeSetLocationFailure(CallbackToFutureAdapter.Completer<Result> completer) {
        log.e("Completing with failure: location update failed");
        finish(completer, Result.failure());
    }

    private void completeSetLocationError(CallbackToFutureAdapter.Completer<Result> completer, Throwable t) {
        log.e(t, "Completing with failure: set location error");
        finish(completer, Result.failure());
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

    @SuppressLint("MissingPermission")
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
                        if (response.getSuccess()) completeSetLocationSuccess(completer);
                        else completeSetLocationFailure(completer);
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
