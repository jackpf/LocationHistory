package com.jackpf.locationhistory.client.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class LocationProvider {
    private static final int LOCATION_TIMEOUT_MS = 30000;
    private final Context context;
    private final LocationManager locationManager;
    private final PermissionsManager permissionsManager;
    private final Logger log = new Logger(this);

    public LocationProvider(Context context, PermissionsManager permissionsManager) {
        this.context = context;
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
        CancellationSignal cancellationSignal = new CancellationSignal();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (resultHandled.compareAndSet(false, true)) {
                log.w("Location request timed out, cancelling...");
                cancellationSignal.cancel();
                consumer.accept(fallbackLocation());
            }
        }, LOCATION_TIMEOUT_MS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            locationManager.getCurrentLocation(
                    LocationManager.GPS_PROVIDER,
                    cancellationSignal,
                    context.getMainExecutor(),
                    location -> {
                        if (resultHandled.compareAndSet(false, true)) {
                            consumer.accept(location);
                        }
                    }
            );
        } else {
            if (resultHandled.compareAndSet(false, true)) {
                consumer.accept(fallbackLocation());
            }
        }
    }
}
