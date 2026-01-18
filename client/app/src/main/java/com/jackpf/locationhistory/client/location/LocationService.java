package com.jackpf.locationhistory.client.location;

import android.content.Context;
import android.location.LocationManager;

import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.util.Logger;

import java.util.ArrayList;
import java.util.Arrays;
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
    private final ExecutorService threadExecutor;
    private final LegacyHighAccuracyProvider legacyHighAccuracyProvider;
    private final LegacyCachedProvider legacyCachedProvider;
    private final OptimisedProvider optimisedProvider;

    private final Logger log = new Logger(this);

    /**
     * GPS takes a bit longer to wake up (~15s for radio wakeup)
     */
    private static final int GPS_TIMEOUT = 30_000;
    /**
     * Network should be relatively fast (~5s for radio wakeup or near instant if using WiFi location)
     */
    private static final int NETWORK_TIMEOUT = 10_000;
    /**
     * Timeout doesn't apply to cache requests
     */
    private static final int CACHE_TIMEOUT = -1;

    @Getter
    @AllArgsConstructor
    private static class ProviderRequest {
        String source;
        int timeout;
        LocationProvider provider;
    }

    LocationService(LocationManager locationManager,
                    PermissionsManager permissionsManager,
                    ExecutorService threadExecutor,
                    LegacyHighAccuracyProvider legacyHighAccuracyProvider,
                    LegacyCachedProvider legacyCachedProvider,
                    OptimisedProvider optimisedProvider) {
        this.locationManager = locationManager;
        this.permissionsManager = permissionsManager;
        this.threadExecutor = threadExecutor;
        this.legacyHighAccuracyProvider = legacyHighAccuracyProvider;
        this.legacyCachedProvider = legacyCachedProvider;
        this.optimisedProvider = optimisedProvider;
    }

    public static LocationService create(Context context, PermissionsManager permissionsManager) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        ExecutorService threadExecutor = Executors.newSingleThreadExecutor();

        return new LocationService(
                locationManager,
                permissionsManager,
                threadExecutor,
                new LegacyHighAccuracyProvider(locationManager, threadExecutor),
                new LegacyCachedProvider(locationManager, threadExecutor),
                new OptimisedProvider(locationManager, threadExecutor)
        );
    }

    private void callProvider(Iterator<ProviderRequest> providers, Consumer<LocationData> consumer) {
        if (!providers.hasNext()) {
            consumer.accept(null); // All providers failed
            return;
        }

        ProviderRequest nextProvider = providers.next();

        if (locationManager.isProviderEnabled(nextProvider.getSource())) {
            nextProvider.getProvider().provide(nextProvider.getSource(), nextProvider.getTimeout(), location -> {
                if (location != null) consumer.accept(location);
                else callProvider(providers, consumer);
            });
        } else {
            callProvider(providers, consumer);
        }
    }

    /**
     * @param accuracy Defines priority of accuracy vs battery use (GPS v.s. network v.s. cached location sources)
     * @param consumer Callback for location data
     * @throws SecurityException If we're missing location permissions
     */
    public void getLocation(RequestedAccuracy accuracy, Consumer<LocationData> consumer) throws SecurityException {
        if (!permissionsManager.hasLocationPermissions()) {
            throw new SecurityException("No location permissions");
        }

        List<ProviderRequest> providers = new ArrayList<>();

        if (optimisedProvider.isSupported()) {
            /* Optimised provider will automatically use the location cache if available
             * and return a fresh (< ~30s) location if available */
            log.d("Using optimised location manager");

            ProviderRequest gpsRequest = new ProviderRequest(LocationManager.GPS_PROVIDER, GPS_TIMEOUT, optimisedProvider);
            ProviderRequest networkRequest = new ProviderRequest(LocationManager.NETWORK_PROVIDER, NETWORK_TIMEOUT, optimisedProvider);

            if (accuracy == RequestedAccuracy.HIGH) {
                providers.addAll(Arrays.asList(gpsRequest, networkRequest));
            } else {
                providers.addAll(Arrays.asList(networkRequest, gpsRequest));
            }
        } else {
            /* The legacy provider will directly request location from the hardware,
             * so we've  gotta implement our own cache checks */
            log.d("Using legacy location manager");

            ProviderRequest highGps = new ProviderRequest(LocationManager.GPS_PROVIDER, GPS_TIMEOUT, legacyHighAccuracyProvider);
            ProviderRequest highNetwork = new ProviderRequest(LocationManager.NETWORK_PROVIDER, NETWORK_TIMEOUT, legacyHighAccuracyProvider);
            ProviderRequest cachedGps = new ProviderRequest(LocationManager.GPS_PROVIDER, CACHE_TIMEOUT, legacyCachedProvider);
            ProviderRequest cachedNetwork = new ProviderRequest(LocationManager.NETWORK_PROVIDER, CACHE_TIMEOUT, legacyCachedProvider);

            if (accuracy == RequestedAccuracy.HIGH) {
                providers.addAll(Arrays.asList(highGps, highNetwork, cachedGps, cachedNetwork));
            } else {
                providers.addAll(Arrays.asList(cachedGps, cachedNetwork, highNetwork, highGps));
            }
        }

        callProvider(providers.iterator(), consumer);
    }

    @Override
    public void close() {
        threadExecutor.shutdown();
    }
}
