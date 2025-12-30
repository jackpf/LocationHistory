package com.jackpf.locationhistory.client.location;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.util.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import lombok.Value;

public class LocationProvider implements AutoCloseable {
    private final LocationManager locationManager;
    private final PermissionsManager permissionsManager;
    private final ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
    private final Logger log = new Logger(this);


    private static final int LOCATION_TIMEOUT_MS = 30000;
    private static final long FRESHNESS_THRESHOLD_MS = (long) 1000 * 60 * 5; // 5 Minutes

    @Value
    public static class LocationData {
        Location location;
        String source;
    }


    public LocationProvider(Context context, PermissionsManager permissionsManager) {
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.permissionsManager = permissionsManager;
    }

    private boolean isFresh(LocationData data) {
        if (data.getLocation() == null) return false;
        long age = System.currentTimeMillis() - data.getLocation().getTime();
        return age < FRESHNESS_THRESHOLD_MS;
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private LocationData getCachedLocation() {
        Location bestLocation = null;
        String source = null;

        for (String provider : locationManager.getAllProviders()) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location == null) continue;

            if (bestLocation == null || location.getTime() > bestLocation.getTime()) {
                bestLocation = location;
                source = provider;
            }
        }

        return new LocationData(bestLocation, source);
    }

    private boolean newLocationManagerSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void getLiveLocation(String provider, LocationData fallbackLocation, Consumer<LocationData> consumer) {
        if (!newLocationManagerSupported())
            throw new RuntimeException("getLiveLocation requires Android R or above");
        log.d("Using updated location manager");

        CancellationSignal cancellationSignal = new CancellationSignal();

        locationManager.getCurrentLocation(
                provider,
                cancellationSignal,
                threadExecutor,
                location -> {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (location != null) {
                            consumer.accept(new LocationData(location, provider));
                        } else {
                            if (fallbackLocation.getLocation() != null)
                                consumer.accept(fallbackLocation);
                        }
                    });
                }
        );

    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void getLegacyLiveLocation(String provider, LocationData fallbackLocation, Consumer<LocationData> consumer) {
        log.d("Using legacy location manager");

        final AtomicBoolean resultHandled = new AtomicBoolean(false);

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (resultHandled.compareAndSet(false, true)) {
                    log.d("Location data received from listener");
                    consumer.accept(new LocationData(location, provider));
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
                consumer.accept(fallbackLocation);
            }
        }, LOCATION_TIMEOUT_MS);

        locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper());
    }

    private String getBestProvider() {
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        } else {
            return null;
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    public void getLocation(Consumer<LocationData> consumer) throws SecurityException {
        if (!permissionsManager.hasLocationPermissions()) {
            throw new SecurityException("No location permissions");
        }

        LocationData cachedLocation = getCachedLocation();
        if (isFresh(cachedLocation)) {
            log.d("Using fresh cached location: %s", cachedLocation);
            consumer.accept(cachedLocation);
            return;
        }

        String provider = getBestProvider();

        if (provider == null) {
            if (cachedLocation.getLocation() != null) {
                log.w("No providers enabled, returning stale cached location");
                consumer.accept(cachedLocation);
            } else log.e("No location providers available");
            return;
        }

        if (newLocationManagerSupported()) getLiveLocation(provider, cachedLocation, consumer);
        else getLegacyLiveLocation(provider, cachedLocation, consumer);
    }

    @Override
    public void close() {
        threadExecutor.shutdown();
    }
}
