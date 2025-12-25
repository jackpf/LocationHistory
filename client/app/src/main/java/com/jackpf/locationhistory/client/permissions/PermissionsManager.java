package com.jackpf.locationhistory.client.permissions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class PermissionsManager {
    private static boolean isGranted(@NonNull Context context, @NonNull String permission) {
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasLocationPermissions(Context context) {
        return isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
                && isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION);
    }
}
