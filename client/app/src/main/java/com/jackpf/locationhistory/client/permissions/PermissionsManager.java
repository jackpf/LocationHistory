package com.jackpf.locationhistory.client.permissions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

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
}
