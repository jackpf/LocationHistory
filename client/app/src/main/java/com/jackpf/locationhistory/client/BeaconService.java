package com.jackpf.locationhistory.client;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.jackpf.locationhistory.PingResponse;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.grpc.BeaconRequest;
import com.jackpf.locationhistory.client.grpc.util.GrpcFutureWrapper;
import com.jackpf.locationhistory.client.location.LocationProvider;
import com.jackpf.locationhistory.client.model.DeviceState;
import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class BeaconService extends Service {
    private ConfigRepository configRepo;
    private SharedPreferences.OnSharedPreferenceChangeListener configListener;
    private Handler handler;
    private LocationProvider locationProvider;
    private BeaconClient beaconClient;
    private PermissionsManager permissionsManager;
    private final IBinder binder = new LocalBinder();
    private static final long CLIENT_TIMEOUT_MILLIS = 10_000;
    private final DeviceState deviceState = new DeviceState();

    private final Logger log = new Logger(this);

    public class LocalBinder extends Binder {
        BeaconService getService() {
            return BeaconService.this;
        }
    }

    @Override
    public IBinder onBind(Intent i) {
        return binder;
    }

    private void failureCallback(StatusRuntimeException exception) {
        switch (exception.getStatus().getCode()) {
            case UNAUTHENTICATED:
            case PERMISSION_DENIED:
            case NOT_FOUND:
                log.e(exception, "gRPC authentication error, resetting device status");
                deviceState.setNotReady();
                break;
            default:
                log.e(exception, "GRPC error");
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

            return new BeaconClient(channel, CLIENT_TIMEOUT_MILLIS);
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

    @Override
    public void onCreate() {
        super.onCreate();
        configRepo = new ConfigRepository(this);
        configListener = this::handleConfigUpdate;
        configRepo.registerOnSharedPreferenceChangeListener(configListener);
        handler = new Handler(Looper.getMainLooper());
        permissionsManager = new PermissionsManager(this);
        locationProvider = new LocationProvider(this, permissionsManager);
        beaconClient = createBeaconClient();
        setDeviceIdIfNotPresent();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        configRepo.unregisterOnSharedPreferenceChangeListener(configListener);
        beaconClient.close();
        locationProvider.close();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log.d("Start command");
        loop(0);
        return START_STICKY;
    }

    private String _getDeviceId() {
        return configRepo.getDeviceId();
    }

    private String _getPublicKey() {
        return configRepo.getPublicKey();
    }

    public void onDeviceStateReady(String deviceId, String publicKey, Runnable callback) {
        // Device has been detected as registered & ready - no need to check again
        if (deviceState.isReady()) {
            callback.run();
            return;
        }

        try {
            getBeaconClient().checkDevice(deviceId, new GrpcFutureWrapper<>(v -> {
                switch (v.getStatus()) {
                    case DEVICE_REGISTERED:
                        log.d("Device %s is registered, device state ready", deviceId);
                        deviceState.setReady();
                        callback.run();
                        break;
                    case DEVICE_PENDING:
                        log.d("Device %s is pending registration", deviceId);
                        deviceState.setNotReady();
                        break;
                    case DEVICE_UNKNOWN:
                    default:
                        log.d("Device %s not found, registering as new", deviceId);

                        try {
                            getBeaconClient().registerDevice(deviceId, publicKey, new GrpcFutureWrapper<>(v2 -> {
                                if (v2.getSuccess()) {
                                    log.d("Device %s successfully registered", deviceId);
                                    deviceState.setReady();
                                    callback.run();
                                } else {
                                    log.w("Device %s failed to be registered", deviceId);
                                    deviceState.setNotReady();
                                }
                            }, this::failureCallback));
                        } catch (IOException e) {
                            log.e("Register device error", e);
                            deviceState.setNotReady();
                        }
                        break;
                }
            }, error -> {
                this.failureCallback(error);
                deviceState.setNotReady(); // Set un-ready for any error
            }));
        } catch (IOException e) {
            log.e("Check device error", e);
            deviceState.setNotReady();
        }
    }

    private void loop(long intervalMillis) {
        handler.postDelayed(() -> {
            persistNotification();
            onDeviceStateReady(configRepo.getDeviceId(), configRepo.getPublicKey(), () -> {
                if (permissionsManager.hasLocationPermissions()) {
                    handleLocationUpdate();
                } else {
                    log.w("Location permissions not granted");
                }
            });
            loop(configRepo.getUpdateIntervalMillis());
        }, intervalMillis);
    }

    private void handleConfigUpdate(SharedPreferences sharedPreferences, String key) {
        log.d("Config update detected");
        beaconClient = createBeaconClient();
        deviceState.setNotReady();
        ; // Force re-checking device state with new config
    }

    @SuppressLint("MissingPermission")
    private void handleLocationUpdate() {
        log.d("Updating location");

        locationProvider.getLocation(locationData -> {
            if (locationData != null) {
                log.d("Received location data: %s", locationData.toString());
                try {
                    getBeaconClient().sendLocation(
                            _getDeviceId(),
                            _getPublicKey(),
                            BeaconRequest.fromLocation(locationData.getLocation()),
                            new GrpcFutureWrapper<>(v -> {
                            }, this::failureCallback)
                    );
                } catch (IOException e) {
                    log.e("Failed to send location", e);
                }
            } else {
                log.w("Received null location data");
            }
        });
    }

    public void testConnection(GrpcFutureWrapper<PingResponse> callback) throws IOException {
        getBeaconClient().ping(callback);
    }

    private void persistNotification() {
        log.d("Updating persistent notification");

        NotificationChannel channel = new NotificationChannel(
                "beacon", "Beacon", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        Notification notification = new Notification.Builder(this, "beacon")
                .setContentTitle("Beacon active")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                .build();

        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
    }
}
