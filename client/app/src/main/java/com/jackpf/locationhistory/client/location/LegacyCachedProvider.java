package com.jackpf.locationhistory.client.location;

import android.Manifest;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.RequiresPermission;

import com.jackpf.locationhistory.client.util.Logger;

import java.util.function.Consumer;

public class LegacyCachedProvider implements LocationProvider {
    private final Logger log = new Logger(this);

    private final LocationManager locationManager;

    private static final long FRESHNESS_THRESHOLD_MS = (long) 1000 * 60 * 5; // 5 Minutes

    public LegacyCachedProvider(LocationManager locationManager) {
        this.locationManager = locationManager;
    }

    private boolean isFresh(LocationData data) {
        if (data.getLocation() == null) return false;
        long age = System.currentTimeMillis() - data.getLocation().getTime();
        return age < FRESHNESS_THRESHOLD_MS;
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private LocationData getCachedLocation(String source) {
        Location location = locationManager.getLastKnownLocation(source);

        return new LocationData(location, source);
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    @Override
    public void provide(String source, int timeout, Consumer<LocationData> consumer) {
        LocationData cachedLocation = getCachedLocation(source);
        if (isFresh(cachedLocation)) {
            log.d("Using fresh cached location: %s", cachedLocation);
            consumer.accept(cachedLocation);
        } else {
            log.d("No cached location available");
            consumer.accept(null);
        }
    }
}
