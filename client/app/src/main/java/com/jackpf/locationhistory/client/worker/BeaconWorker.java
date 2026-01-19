package com.jackpf.locationhistory.client.worker;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.location.LocationService;
import com.jackpf.locationhistory.client.model.DeviceState;
import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.service.DeviceStateService;
import com.jackpf.locationhistory.client.service.LocationUpdateService;
import com.jackpf.locationhistory.client.util.Logger;

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

    public BeaconWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);

        configRepository = new ConfigRepository(getApplicationContext());
        deviceState = DeviceState.fromConfig(configRepository);
        log.i("Created device state with device ID: %s", deviceState.getDeviceId());

        permissionsManager = new PermissionsManager(getApplicationContext());
        locationService = LocationService.create(getApplicationContext(), permissionsManager);
        backgroundExecutor = Executors.newSingleThreadExecutor();
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        log.d("Beacon triggered");
        log.appendEventToFile(getApplicationContext(), "Beacon triggered");


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
}
