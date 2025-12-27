package com.jackpf.locationhistory.client;

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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.grpc.BeaconRequest;
import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.util.Log;

import java.io.IOException;
import java.util.UUID;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class BeaconService extends Service {
    private ConfigRepository configRepo;
    private SharedPreferences.OnSharedPreferenceChangeListener configListener;
    private Handler handler;
    private FusedLocationProviderClient locationProvider;
    private BeaconClient beaconClient;
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        BeaconService getService() {
            return BeaconService.this;
        }
    }

    @Override
    public IBinder onBind(Intent i) {
        return binder;
    }

    private BeaconClient createBeaconClient() {
        Log.d("Connecting to server %s:%d".formatted(configRepo.getServerHost(), configRepo.getServerPort()));

        try {
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress(configRepo.getServerHost(), configRepo.getServerPort())
                    .usePlaintext()
                    .build();

            return new BeaconClient(channel);
        } catch (IllegalArgumentException e) {
            Log.e("Invalid server details", e);
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
            Log.d("Device ID not present, setting as %s".formatted(newDeviceId));
            configRepo.setDeviceId(newDeviceId);
        } else {
            Log.d("Device ID is %s".formatted(currentDeviceId));
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        configRepo = new ConfigRepository(this);
        configListener = this::handleConfigUpdate;
        configRepo.registerOnSharedPreferenceChangeListener(configListener);
        handler = new Handler(Looper.getMainLooper());
        locationProvider = LocationServices.getFusedLocationProviderClient(this);
        beaconClient = createBeaconClient();
        setDeviceIdIfNotPresent();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        configRepo.unregisterOnSharedPreferenceChangeListener(configListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Start command");
        loop();
        return START_STICKY;
    }

    private String _getDeviceId() {
        return configRepo.getDeviceId();
    }

    private String _getPublicKey() {
        return configRepo.getPublicKey();
    }

    private void loop() {
        handler.postDelayed(() -> {
            persistNotification();
            handleLocationUpdate();
            loop();
        }, configRepo.getUpdateIntervalMillis());
    }

    private void handleConfigUpdate(SharedPreferences sharedPreferences, String key) {
        Log.d("Config update detected");
        beaconClient = createBeaconClient();
    }

    private void handleLocationUpdate() {
        Log.d("Update location");

        if (PermissionsManager.hasLocationPermissions(this)) {
            Log.d("Requesting location data");

            locationProvider.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    Log.d("Received location data: %s".formatted(location.toString()));
                    try {
                        getBeaconClient().sendLocation(
                                _getDeviceId(),
                                _getPublicKey(),
                                BeaconRequest.fromLocation(location)
                        );
                    } catch (IOException e) {
                        Log.e("Failed to send location", e);
                    }
                } else {
                    Log.w("Received null location data");
                }
            });
        } else {
            Log.e("Missing location permissions");
        }
    }

    public boolean testConnection() {
        try {
            getBeaconClient().ping();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void persistNotification() {
        Log.d("Starting persistent notification");

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
