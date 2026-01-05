package com.jackpf.locationhistory.client;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.DeviceStatus;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.grpc.BeaconRequest;
import com.jackpf.locationhistory.client.location.LocationProvider;
import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class BeaconWorker extends ListenableWorker {
    private final ConfigRepository configRepo;
    private final LocationProvider locationProvider;
    private final BeaconClient beaconClient;
    private final PermissionsManager permissionsManager;
    private final Executor backgroundExecutor;
    private static final long CLIENT_TIMEOUT_MILLIS = 10_000;
    // True if device state has been detected as ready - we no longer check once we detect this
    // TODO Persist
    private boolean deviceStateReady = false;

    private final Logger log = new Logger(this);

    public BeaconWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);

        configRepo = new ConfigRepository(getApplicationContext());
        permissionsManager = new PermissionsManager(getApplicationContext());
        locationProvider = new LocationProvider(getApplicationContext(), permissionsManager);
        beaconClient = createBeaconClient();
        backgroundExecutor = Executors.newSingleThreadExecutor();
        setDeviceIdIfNotPresent();
    }

    private void failureCallback(StatusRuntimeException exception) {
        switch (exception.getStatus().getCode()) {
            case UNAUTHENTICATED:
            case PERMISSION_DENIED:
            case NOT_FOUND:
                log.w("gRPC authentication error, resetting device status", exception);
                deviceStateReady = false;
                break;
        }
    }

    private BeaconClient createBeaconClient() {
        log.d("Connecting to server %s:%d", configRepo.getServerHost(), configRepo.getServerPort());

        try {
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress(configRepo.getServerHost(), configRepo.getServerPort())
                    .idleTimeout(15, TimeUnit.SECONDS)
                    .keepAliveWithoutCalls(false)
                    .usePlaintext()
                    .build();

            return new BeaconClient(channel, CLIENT_TIMEOUT_MILLIS, this::failureCallback);
        } catch (IllegalArgumentException e) {
            log.e("Invalid server details", e);
            return null;
        }
    }

    private BeaconClient getBeaconClient() throws IOException {
        if (beaconClient != null) return beaconClient;
        else throw new IOException("Client not connected");
    }

    private void setDeviceIdIfNotPresent() {
        String currentDeviceId = configRepo.getDeviceId();

        if ("".equals(currentDeviceId)) {
            String newDeviceId = UUID.randomUUID().toString();
            log.d("Device ID not present, setting as %s", newDeviceId);
            configRepo.setDeviceId(newDeviceId);
        } else {
            log.d("Device ID is %s", currentDeviceId);
        }
    }

    private String _getDeviceId() {
        return configRepo.getDeviceId();
    }

    private String _getDeviceName() {
        String deviceName = Settings.Global.getString(getApplicationContext().getContentResolver(), Settings.Global.DEVICE_NAME);
        return deviceName != null ? deviceName : "";
    }

    private String _getPublicKey() {
        return configRepo.getPublicKey();
    }

    public boolean checkDeviceState() {
        // Device has been detected as registered & ready - no need to check again
        if (deviceStateReady) {
            return true;
        }

        String deviceId = _getDeviceId();
        String deviceName = _getDeviceName();
        String publicKey = _getPublicKey();

        try {
            DeviceStatus deviceStatus = getBeaconClient().checkDevice(deviceId);
            switch (deviceStatus) {
                case DEVICE_REGISTERED:
                    log.d("Device %s is registered, device state ready", deviceId);
                    deviceStateReady = true;
                    break;
                case DEVICE_PENDING:
                    log.d("Device %s is pending registration", deviceId);
                    deviceStateReady = false;
                    break;
                case DEVICE_UNKNOWN:
                default:
                    log.d("Device %s not found, registering as new", deviceId);
                    if (!getBeaconClient().registerDevice(deviceId, deviceName, publicKey)) {
                        log.e("Device registration unsuccessful");
                    }
                    deviceStateReady = false;
                    break;
            }
        } catch (IOException e) {
            log.e("Check device error", e);
            deviceStateReady = false;
        }
        return deviceStateReady;
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            backgroundExecutor.execute(() -> {
                try {
                    if (!permissionsManager.hasLocationPermissions()) {
                        log.e("No location permissions");
                        finish(completer, Result.success());
                    } else if (!checkDeviceState()) {
                        log.w("Device not ready, retrying...");
                        finish(completer, Result.retry());
                    } else {
                        log.d("Device ready, handling location update");
                        handleLocationUpdate(completer);
                    }
                } catch (Exception e) {
                    log.e(e, "Failure");
                    finish(completer, Result.failure());
                }
            });

            return "BeaconWorkerFuture";
        });
    }

    @SuppressLint("MissingPermission")
    private void handleLocationUpdate(CallbackToFutureAdapter.Completer<Result> completer) {
        log.d("Updating location");

        locationProvider.getLocation(locationData -> {
            if (locationData != null) {
                log.d("Received location data: %s", locationData.toString());
                try {
                    getBeaconClient().sendLocation(
                            _getDeviceId(),
                            _getPublicKey(),
                            BeaconRequest.fromLocation(locationData.getLocation())
                    );
                    finish(completer, Result.success());
                } catch (IOException e) {
                    log.e("Failed to send location", e);
                    finish(completer, Result.retry());
                }
            } else {
                log.w("Received null location data");
                finish(completer, Result.retry());
            }
        });
    }

    private void finish(CallbackToFutureAdapter.Completer<Result> completer, Result result) {
        try {
            close();
        } catch (Exception e) {
            log.e("Error closing resources", e);
        } finally {
            completer.set(result);
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        log.d("Worker stopped by system");
        close();
    }

    private void close() {
        beaconClient.close();
        locationProvider.close();
    }
}
