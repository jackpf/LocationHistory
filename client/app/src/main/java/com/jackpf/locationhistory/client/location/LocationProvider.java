package com.jackpf.locationhistory.client.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class LocationProvider {
    private static final int LOCATION_TIMEOUT_MS = 30000;
    private final LocationManager locationManager;
    private final PermissionsManager permissionsManager;
    private final Logger log = new Logger(this);

    public LocationProvider(Context context, PermissionsManager permissionsManager) {
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.permissionsManager = permissionsManager;
    }

    private boolean gpsEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @SuppressLint("MissingPermission")
    private Location fallbackLocation() {
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    @SuppressLint("MissingPermission")
    public void getLocation(Consumer<Location> consumer) throws IOException {
        if (!permissionsManager.hasLocationPermissions()) {
            throw new IOException("No location permissions");
        }

        if (!gpsEnabled()) {
            throw new IOException("GPS is not enabled");
        }

        final AtomicBoolean resultHandled = new AtomicBoolean(false);

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (resultHandled.compareAndSet(false, true)) {
                    log.d("Location data received from listener");
                    consumer.accept(location);
                    locationManager.removeUpdates(this);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
            }
        };

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (resultHandled.compareAndSet(false, true)) {
                log.w("Location request timed out; using fallback location");
                locationManager.removeUpdates(listener);
                consumer.accept(fallbackLocation());
            }
        }, LOCATION_TIMEOUT_MS);

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0,
                0,
                listener,
                Looper.getMainLooper()
        );
    }
}
