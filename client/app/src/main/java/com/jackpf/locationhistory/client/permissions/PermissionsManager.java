package com.jackpf.locationhistory.client.permissions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class PermissionsManager {
    private final Context context;

    public PermissionsManager(Context context) {
        this.context = context;
    }

    private boolean isGranted(@NonNull String permission) {
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean hasLocationPermissions() {
        return isGranted(Manifest.permission.ACCESS_FINE_LOCATION)
                && isGranted(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    public boolean hasBackgroundPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(android.content.Context.POWER_SERVICE);
            return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true;
    }
}
