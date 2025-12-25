package com.jackpf.locationhistory.client;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.jackpf.locationhistory.client.http.BeaconClient;
import com.jackpf.locationhistory.client.http.BeaconRequest;
import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.util.Log;

import tools.jackson.databind.ObjectMapper;

public class BeaconService extends Service {
    // TODO Make configurable
    private static final int BEACON_INTERVAL = 10_000;

    private Handler handler;
    private FusedLocationProviderClient locationProvider;
    private BeaconClient beaconClient;

    @Override
    public void onCreate() {
        handler = new Handler(Looper.getMainLooper());
        locationProvider = LocationServices.getFusedLocationProviderClient(this);
        beaconClient = new BeaconClient(new ObjectMapper());
    }

    @Override
    public int onStartCommand(Intent i, int f, int id) {
        Log.d("Started");

        persistNotification();
        loop();
        return START_STICKY;
    }

    private void loop() {
        handler.postDelayed(() -> {
            ping();
            persistNotification();
            loop();
        }, BEACON_INTERVAL);
    }

    private void ping() {
        Log.d("Ping");

        if (PermissionsManager.hasLocationPermissions(this)) {
            Log.d("Requesting location data");

            locationProvider.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    Log.d("Received location data: %s".formatted(location.toString()));
                    beaconClient.send(BeaconRequest.fromLocation(location));
                } else {
                    Log.w("Received null location data");
                }
            });
        } else {
            Log.e("Missing location permissions");
        }
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
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
