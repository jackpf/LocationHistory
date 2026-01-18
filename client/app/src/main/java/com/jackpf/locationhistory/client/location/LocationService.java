package com.jackpf.locationhistory.client.location;

import android.content.Context;
import android.location.LocationManager;

import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.util.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class LocationService implements AutoCloseable {
    private final LocationManager locationManager;
    private final PermissionsManager permissionsManager;
    private final LegacyHighAccuracyProvider legacyHighAccuracyProvider;
    private final LegacyCachedProvider legacyCachedProvider;
    private final OptimisedProvider optimisedProvider;
    private final ExecutorService threadExecutor = Executors.newSingleThreadExecutor();

    private final Logger log = new Logger(this);

    @Getter
    @AllArgsConstructor
    private static class SourceAndProvider {
        String source;
        LocationProvider provider;
    }

    public LocationService(Context context, PermissionsManager permissionsManager) {
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.permissionsManager = permissionsManager;
        this.legacyHighAccuracyProvider = new LegacyHighAccuracyProvider(locationManager);
        this.legacyCachedProvider = new LegacyCachedProvider(locationManager);
        this.optimisedProvider = new OptimisedProvider(locationManager, threadExecutor);
    }

    private int getTimeout() {
        return 30_000;
    }

    private void callProvider(Iterator<SourceAndProvider> providers, Consumer<LocationData> consumer) {
        if (!providers.hasNext()) {
            consumer.accept(null); // All providers failed
            return;
        }

        SourceAndProvider nextEntry = providers.next();

        if (locationManager.isProviderEnabled(nextEntry.getSource())) {
            nextEntry.getProvider().provide(nextEntry.getSource(), getTimeout(), location -> {
                if (location != null) consumer.accept(location);
                else callProvider(providers, consumer);
            });
        } else {
            callProvider(providers, consumer);
        }
    }

    public void getLocation(RequestedAccuracy accuracy, Consumer<LocationData> consumer) throws SecurityException {
        if (!permissionsManager.hasLocationPermissions()) {
            throw new SecurityException("No location permissions");
        }

        List<SourceAndProvider> providers = new ArrayList<>();

        if (optimisedProvider.isSupported()) {
            /* Optimised provider will automatically use the location cache if available
             * and return a fresh (< ~30s) location if available */
            if (accuracy == RequestedAccuracy.HIGH) {
                providers.add(new SourceAndProvider(LocationManager.GPS_PROVIDER, optimisedProvider));
                providers.add(new SourceAndProvider(LocationManager.NETWORK_PROVIDER, optimisedProvider));
            } else {
                providers.add(new SourceAndProvider(LocationManager.NETWORK_PROVIDER, optimisedProvider));
                providers.add(new SourceAndProvider(LocationManager.GPS_PROVIDER, optimisedProvider));
            }
        } else {
            /* The legacy provider will directly request location from the hardware,
             * so we've  gotta implement our own cache checks (unless we're on high accuracy) */
            if (accuracy == RequestedAccuracy.HIGH) {
                providers.add(new SourceAndProvider(LocationManager.GPS_PROVIDER, legacyHighAccuracyProvider));
                providers.add(new SourceAndProvider(LocationManager.NETWORK_PROVIDER, legacyHighAccuracyProvider));
                providers.add(new SourceAndProvider(LocationManager.GPS_PROVIDER, legacyCachedProvider));
                providers.add(new SourceAndProvider(LocationManager.NETWORK_PROVIDER, legacyCachedProvider));
            } else {
                providers.add(new SourceAndProvider(LocationManager.GPS_PROVIDER, legacyCachedProvider));
                providers.add(new SourceAndProvider(LocationManager.NETWORK_PROVIDER, legacyCachedProvider));
                providers.add(new SourceAndProvider(LocationManager.GPS_PROVIDER, legacyHighAccuracyProvider));
                providers.add(new SourceAndProvider(LocationManager.NETWORK_PROVIDER, legacyHighAccuracyProvider));
            }
        }

        callProvider(providers.iterator(), consumer);
    }

    @Override
    public void close() {
        threadExecutor.shutdown();
    }
}
