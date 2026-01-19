package com.jackpf.locationhistory.client.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.Data;
import androidx.work.ListenableWorker;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.SetLocationResponse;
import com.jackpf.locationhistory.client.client.BeaconClientFactory;
import com.jackpf.locationhistory.client.client.ssl.TrustedCertStorage;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.location.LocationData;
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
import java.util.function.BiConsumer;

public class BeaconTask implements AutoCloseable {
    private final Context context;
    private final ConfigRepository configRepository;
    private final DeviceState deviceState;
    private final LocationService locationService;
    private BeaconClient beaconClient;
    private final PermissionsManager permissionsManager;
    private final ExecutorService backgroundExecutor;

    // Services
    private DeviceStateService deviceStateService;
    private LocationUpdateService locationUpdateService;

    private static final String BEACON_TASK_NAME = "BeaconTask";
    public static final String DATA_RUN_TIMESTAMP = "BeaconWorkerRunTimestamp";
    public static final String DATA_MESSAGE = "BeaconWorkerMessage";

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

    public BeaconTask(@NonNull Context context,
                      ConfigRepository configRepository,
                      DeviceState deviceState,
                      PermissionsManager permissionsManager,
                      LocationService locationService,
                      ExecutorService backgroundExecutor) {
        this.context = context;
        this.configRepository = configRepository;
        this.deviceState = deviceState;
        this.permissionsManager = permissionsManager;
        this.locationService = locationService;
        this.backgroundExecutor = backgroundExecutor;
    }

    public static BeaconTask create(@NonNull Context context) {
        ConfigRepository configRepository = new ConfigRepository(context);
        DeviceState deviceState = DeviceState.fromConfig(configRepository);
        PermissionsManager permissionsManager = new PermissionsManager(context);
        LocationService locationService = LocationService.create(context, permissionsManager);
        ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

        return new BeaconTask(
                context,
                configRepository,
                deviceState,
                permissionsManager,
                locationService,
                backgroundExecutor
        );
    }

    private Data completeData(String message, boolean updateRunTimestamp) {
        Data.Builder builder = new Data.Builder();
        builder.putString(DATA_MESSAGE, message);
        if (updateRunTimestamp)
            builder.putLong(DATA_RUN_TIMESTAMP, System.currentTimeMillis());
        return builder.build();
    }

    private void completeWithSuccess(CallbackToFutureAdapter.Completer<ListenableWorker.Result> completer, CompleteReason reason) {
        String message = String.format("Completing with success: %s", reason.message);
        log.i(message);
        log.appendEventToFile(context, message);
        finish(completer, ListenableWorker.Result.success(completeData(message, true)));
    }

    private void completeWithRetry(CallbackToFutureAdapter.Completer<ListenableWorker.Result> completer, CompleteReason reason) {
        String message = String.format("Completing with retry: %s", reason.message);
        log.w(message);
        log.appendEventToFile(context, message);
        finish(completer, ListenableWorker.Result.retry());
    }

    private void completeWithFailure(CallbackToFutureAdapter.Completer<ListenableWorker.Result> completer, CompleteReason reason, Throwable t) {
        String message = String.format("Completing with failure: %s", reason.message);
        log.e(t, message);
        log.appendEventToFile(context, message);
        finish(completer, ListenableWorker.Result.failure(completeData(message, false)));
    }

    public ListenableFuture<ListenableWorker.Result> run() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            backgroundExecutor.execute(new SafeRunnable(context, completer, () -> {
                try {
                    BeaconClientFactory.BeaconClientParams params = new BeaconClientFactory.BeaconClientParams(
                            configRepository.getServerHost(),
                            configRepository.getServerPort(),
                            true,
                            BeaconClientFactory.DEFAULT_TIMEOUT
                    );

                    beaconClient = BeaconClientFactory.createPooledClient(params, new TrustedCertStorage(context));
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

                Futures.addCallback(deviceStateService.onDeviceStateReady(deviceState), new SafeCallback<DeviceState>(context, completer) {
                    @Override
                    public void onSafeSuccess(DeviceState state) {
                        if (state.isReady()) requestLocationUpdate(
                                completer,
                                BeaconTask.this::handleLocationUpdate
                        );
                        else completeWithRetry(completer, CompleteReason.DEVICE_NOT_READY);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        completeWithFailure(completer, CompleteReason.DEVICE_CHECK_ERROR, t);
                    }
                }, backgroundExecutor);
            }));

            return BEACON_TASK_NAME;
        });
    }

    private void requestLocationUpdate(CallbackToFutureAdapter.Completer<ListenableWorker.Result> completer,
                                       BiConsumer<CallbackToFutureAdapter.Completer<ListenableWorker.Result>, LocationData> onResult) {
        log.d("Updating location");

        locationService.getLocation(RequestedAccuracy.BALANCED, locationData ->
                new SafeRunnable(context, completer, () -> onResult.accept(completer, locationData)).run()
        );
    }

    private void handleLocationUpdate(CallbackToFutureAdapter.Completer<ListenableWorker.Result> completer, LocationData locationData) {
        if (locationData != null) {
            log.d("Received location data: %s", locationData.toString());

            Futures.addCallback(locationUpdateService.setLocation(
                    deviceState,
                    locationData
            ), new SafeCallback<SetLocationResponse>(context, completer) {
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
    }

    private void finish(CallbackToFutureAdapter.Completer<ListenableWorker.Result> completer, ListenableWorker.Result result) {
        try {
            close();
        } finally {
            deviceState.storeToConfig(configRepository);
            completer.set(result);
        }
    }

    @Override
    public void close() {
        log.d("Closing resources");
        try {
            locationService.close();
            backgroundExecutor.shutdown();
        } catch (Exception e) {
            log.e("Error closing resources", e);
        }
    }
}
