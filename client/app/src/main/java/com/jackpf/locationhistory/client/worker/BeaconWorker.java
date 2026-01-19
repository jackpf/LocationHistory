package com.jackpf.locationhistory.client.worker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.SetLocationResponse;
import com.jackpf.locationhistory.client.client.BeaconClientFactory;
import com.jackpf.locationhistory.client.client.ssl.TrustedCertStorage;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.location.LocationService;
import com.jackpf.locationhistory.client.location.RequestedAccuracy;
import com.jackpf.locationhistory.client.model.DeviceState;
import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.service.DeviceStateService;
import com.jackpf.locationhistory.client.service.LocationUpdateService;
import com.jackpf.locationhistory.client.util.Logger;
import com.jackpf.locationhistory.client.util.SafeCallback;
import com.jackpf.locationhistory.client.util.SafeRunnable;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BeaconWorker extends ListenableWorker {
    private final ConfigRepository configRepository;
    private final DeviceState deviceState;
    private final LocationService locationService;
    private BeaconClient beaconClient;
    private final PermissionsManager permissionsManager;
    private final ExecutorService backgroundExecutor;

    // Services
    private DeviceStateService deviceStateService;
    private LocationUpdateService locationUpdateService;

    private static final String WORKER_FUTURE_NAME = "BeaconWorkerFuture";
    public static final String WORKER_DATA_RUN_TIMESTAMP = "BeaconWorkerRunTimestamp";
    public static final String WORKER_DATA_MESSAGE = "BeaconWorkerMessage";

    private final Logger log = new Logger(this);

    private enum CompleteReason {
        LOCATION_UPDATED("Location updated"),
        NO_CONNECTION("No connection available"),
        NO_LOCATION_PERMISSIONS("No location permissions"),
        DEVICE_NOT_READY("Device not ready"),
        DEVICE_CHECK_ERROR("Device check error"),
        EMPTY_LOCATION_DATA("Empty location data"),
        SET_LOCATION_FAILED("Location update failed"),
        SET_LOCATION_ERROR("Set location error");

        private final String message;

        CompleteReason(String message) {
            this.message = message;
        }
    }

    public BeaconWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);

        configRepository = new ConfigRepository(getApplicationContext());
        deviceState = DeviceState.fromConfig(configRepository);
        log.i("Created device state with device ID: %s", deviceState.getDeviceId());

        permissionsManager = new PermissionsManager(getApplicationContext());
        locationService = LocationService.create(getApplicationContext(), permissionsManager);
        backgroundExecutor = Executors.newSingleThreadExecutor();
    }

    private Data completeData(String message, boolean updateRunTimestamp) {
        Data.Builder builder = new Data.Builder();
        builder.putString(WORKER_DATA_MESSAGE, message);
        if (updateRunTimestamp)
            builder.putLong(WORKER_DATA_RUN_TIMESTAMP, System.currentTimeMillis());
        return builder.build();
    }

    private void completeWithSuccess(CallbackToFutureAdapter.Completer<Result> completer, CompleteReason reason) {
        String message = String.format("Completing with success: %s", reason.message);
        log.i(message);
        log.appendEventToFile(getApplicationContext(), message);
        finish(completer, Result.success(completeData(message, true)));
    }

    private void completeWithRetry(CallbackToFutureAdapter.Completer<Result> completer, CompleteReason reason) {
        String message = String.format("Completing with retry: %s", reason.message);
        log.w(message);
        log.appendEventToFile(getApplicationContext(), message);
        finish(completer, Result.retry());
    }

    private void completeWithFailure(CallbackToFutureAdapter.Completer<Result> completer, CompleteReason reason, Throwable t) {
        String message = String.format("Completing with failure: %s", reason.message);
        log.e(t, message);
        log.appendEventToFile(getApplicationContext(), message);
        finish(completer, Result.failure(completeData(message, false)));
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        log.d("Beacon triggered");
        log.appendEventToFile(getApplicationContext(), "Beacon triggered");

        return CallbackToFutureAdapter.getFuture(completer -> {
            backgroundExecutor.execute(new SafeRunnable(completer, getApplicationContext(), () -> {
                try {
                    BeaconClientFactory.BeaconClientParams params = new BeaconClientFactory.BeaconClientParams(
                            configRepository.getServerHost(),
                            configRepository.getServerPort(),
                            true,
                            BeaconClientFactory.DEFAULT_TIMEOUT
                    );

                    beaconClient = BeaconClientFactory.createPooledClient(params, new TrustedCertStorage(getApplicationContext()));
                } catch (IOException e) {
                    completeWithFailure(completer, CompleteReason.NO_CONNECTION, e);
                    return;
                }

                deviceStateService = new DeviceStateService(beaconClient, backgroundExecutor);
                locationUpdateService = new LocationUpdateService(beaconClient, backgroundExecutor);

                if (!permissionsManager.hasLocationPermissions()) {
                    completeWithFailure(completer, CompleteReason.NO_LOCATION_PERMISSIONS, null);
                    return;
                }

                Futures.addCallback(deviceStateService.onDeviceStateReady(deviceState), new SafeCallback<DeviceState>(completer, getApplicationContext()) {
                    @Override
                    @SuppressLint("MissingPermission")
                    public void onSafeSuccess(DeviceState state) {
                        if (state.isReady()) handleLocationUpdate(completer);
                        else completeWithRetry(completer, CompleteReason.DEVICE_NOT_READY);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        completeWithFailure(completer, CompleteReason.DEVICE_CHECK_ERROR, t);
                    }
                }, backgroundExecutor);
            }));

            return WORKER_FUTURE_NAME;
        });
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void handleLocationUpdate(CallbackToFutureAdapter.Completer<Result> completer) {
        log.d("Updating location");

        locationService.getLocation(RequestedAccuracy.BALANCED, locationData ->
                new SafeRunnable(completer, getApplicationContext(), () -> {
                    if (locationData != null) {
                        log.d("Received location data: %s", locationData.toString());

                        Futures.addCallback(locationUpdateService.setLocation(
                                deviceState,
                                locationData
                        ), new SafeCallback<SetLocationResponse>(completer, getApplicationContext()) {
                            @Override
                            public void onSafeSuccess(SetLocationResponse response) {
                                if (response.getSuccess()) {
                                    deviceState.setLastRunTimestamp(System.currentTimeMillis());
                                    completeWithSuccess(completer, CompleteReason.LOCATION_UPDATED);
                                } else {
                                    completeWithFailure(completer, CompleteReason.SET_LOCATION_FAILED, null);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Throwable t) {
                                completeWithFailure(completer, CompleteReason.SET_LOCATION_ERROR, t);
                            }
                        }, backgroundExecutor);
                    } else completeWithFailure(completer, CompleteReason.EMPTY_LOCATION_DATA, null);
                }).run()
        );
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

        int stopReason = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            stopReason = getStopReason();
        }
        log.d("Worker stopped");
        log.appendEventToFile(getApplicationContext(), "Finished with onStopped, reason: %s", stopReason);

        close();
    }

    private void close() {
        log.d("Closing resources");
        try {
            locationService.close();
            backgroundExecutor.shutdown();
        } catch (Exception e) {
            log.e("Error closing resources", e);
        }
    }
}
