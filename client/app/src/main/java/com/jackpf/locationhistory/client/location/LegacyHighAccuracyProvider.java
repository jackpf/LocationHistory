package com.jackpf.locationhistory.client.location;

import android.Manifest;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.jackpf.locationhistory.client.util.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class LegacyHighAccuracyProvider implements LocationProvider {
    private final Logger log = new Logger(this);

    private final LocationManager locationManager;

    public LegacyHighAccuracyProvider(LocationManager locationManager) {
        this.locationManager = locationManager;
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    @Override
    public void provide(String source, int timeout, Consumer<LocationData> consumer) {
        final AtomicBoolean resultHandled = new AtomicBoolean(false);

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (resultHandled.compareAndSet(false, true)) {
                    log.d("Location data received from listener");
                    consumer.accept(new LocationData(location, source));
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
                log.w("Location request timed out");
                locationManager.removeUpdates(listener);
                consumer.accept(null);
            }
        }, timeout);

        locationManager.requestSingleUpdate(source, listener, Looper.getMainLooper());
    }
}
