package com.jackpf.locationhistory.client.location;

import android.content.Context;
import android.location.LocationManager;

import com.google.common.collect.Iterators;
import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.util.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class LocationService {
    private final LocationManager locationManager;
    private final PermissionsManager permissionsManager;
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
        private String source;
        private int timeout;
        private LocationProvider provider;
    }

    LocationService(LocationManager locationManager,
                    PermissionsManager permissionsManager,
                    LegacyHighAccuracyProvider legacyHighAccuracyProvider,
                    LegacyCachedProvider legacyCachedProvider,
                    OptimisedProvider optimisedProvider) {
        this.locationManager = locationManager;
        this.permissionsManager = permissionsManager;
        this.legacyHighAccuracyProvider = legacyHighAccuracyProvider;
        this.legacyCachedProvider = legacyCachedProvider;
        this.optimisedProvider = optimisedProvider;
    }

    public static LocationService create(Context context,
                                         Executor threadExecutor) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        return new LocationService(
                locationManager,
                new PermissionsManager(context),
                new LegacyHighAccuracyProvider(locationManager, threadExecutor),
                new LegacyCachedProvider(locationManager, threadExecutor),
                new OptimisedProvider(locationManager, threadExecutor)
        );
    }

    private void callSequentialProviders(Iterator<ProviderRequest> providers, Consumer<LocationData> consumer) {
        if (!providers.hasNext()) {
            consumer.accept(null); // All providers invalid/failed
            return;
        }

        ProviderRequest nextProvider = providers.next();

        if (locationManager.isProviderEnabled(nextProvider.getSource())) {
            nextProvider.getProvider().provide(nextProvider.getSource(), nextProvider.getTimeout(), location -> {
                if (location != null) consumer.accept(location);
                else callSequentialProviders(providers, consumer);
            });
        } else {
            callSequentialProviders(providers, consumer);
        }
    }

    private void callParallelProviders(Iterator<ProviderRequest> providers,
                                       Function<List<LocationData>, LocationData> selector,
                                       Consumer<LocationData> consumer) {
        List<ProviderRequest> validProviders = Arrays.stream(Iterators.toArray(providers, ProviderRequest.class))
                .filter(p -> locationManager.isProviderEnabled(p.source))
                .collect(Collectors.toList());

        if (validProviders.isEmpty()) {
            consumer.accept(null); // All providers invalid
            return;
        }

        Consumer<List<LocationData>> parentConsumer = locations -> consumer.accept(selector.apply(locations));
        ConsumerAggregator<LocationData> aggregator = new ConsumerAggregator<>(validProviders.size(), parentConsumer);

        for (ProviderRequest request : validProviders) {
            request.provider.provide(request.getSource(), request.getTimeout(), aggregator.newChildConsumer());
        }
    }

    private LocationData chooseBestLocation(List<LocationData> locations) {
        LocationData best = null;
        for (LocationData location : locations) {
            if (location.getLocation() != null
                    && (best == null || location.getLocation().getAccuracy() < best.getLocation().getAccuracy())) {
                best = location;
            }
        }
        return best;
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
                callParallelProviders(providers.iterator(), this::chooseBestLocation, consumer);
            } else {
                providers.addAll(Arrays.asList(networkRequest, gpsRequest));
                callSequentialProviders(providers.iterator(), consumer);
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
                callParallelProviders(providers.iterator(), this::chooseBestLocation, consumer);
            } else {
                providers.addAll(Arrays.asList(cachedGps, cachedNetwork, highNetwork, highGps));
                callSequentialProviders(providers.iterator(), consumer);
            }
        }
    }
}
