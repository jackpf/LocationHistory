package com.jackpf.locationhistory.client.location;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;

import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.util.Logger;

public class PassiveLocationListener {
    private final Logger log = new Logger(this);

    private final LocationManager locationManager;
    private final PermissionsManager permissionsManager;
    private final PendingIntent locationIntent;

    public PassiveLocationListener(Context context, PermissionsManager permissionsManager, String action, Class<?> cls) {
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.permissionsManager = permissionsManager;

        Intent intent = new Intent(context, cls);
        intent.setAction(action);
        this.locationIntent = PendingIntent.getService(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0)
        );
    }

    @SuppressLint("MissingPermission")
    public void startMonitoring(long minTimeMs, float minDistanceM) throws SecurityException {
        if (!permissionsManager.hasLocationPermissions()) {
            throw new SecurityException("No permission for passive location");
        }

        log.i("Registering passive location listener");

        locationManager.requestLocationUpdates(
                LocationManager.PASSIVE_PROVIDER,
                minTimeMs,
                minDistanceM,
                locationIntent
        );
    }

    public void stopMonitoring() {
        log.i("Removing passive location listener");
        locationManager.removeUpdates(locationIntent);
    }
}
