package com.jackpf.locationhistory.client.location;

import android.content.Context;
import android.location.LocationManager;

import com.google.common.collect.Streams;
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
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

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
     * If we don't know what we're calling
     */
    private static final int DEFAULT_TIMEOUT = 30_000;
    /**
     * Timeout doesn't apply to cache requests
     */
    private static final int CACHE_TIMEOUT = -1;

    @Getter
    @AllArgsConstructor
    @ToString
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

    public List<String> getAvailableSources() {
        return filterEnabledSources(locationManager.getAllProviders()).stream()
                .filter(p -> !p.equals(LocationManager.PASSIVE_PROVIDER))
                .collect(Collectors.toList());
    }

    private List<String> filterEnabledSources(List<String> sources) {
        return sources.stream()
                .filter(locationManager::isProviderEnabled)
                .collect(Collectors.toList());
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
        List<ProviderRequest> validProviders = Streams.stream(providers)
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
            if (location != null && location.getLocation() != null
                    && (best == null || location.getLocation().getAccuracy() < best.getLocation().getAccuracy())) {
                best = location;
            }
        }
        return best;
    }

    private List<ProviderRequest> createRequests(List<String> sources, LocationProvider provider) {
        List<ProviderRequest> requests = new ArrayList<>();

        for (String source : filterEnabledSources(sources)) {
            int timeout = timeoutForSource(source);
            requests.add(new ProviderRequest(source, timeout, provider));
        }

        return requests;
    }

    private int timeoutForSource(String source) {
        switch (source) {
            case LocationManager.GPS_PROVIDER:
                return GPS_TIMEOUT;
            case LocationManager.NETWORK_PROVIDER:
                return NETWORK_TIMEOUT;
            default:
                return DEFAULT_TIMEOUT;
        }
    }

    /**
     * @param accuracy Defines priority of accuracy vs battery use (GPS v.s. network v.s. cached location sources)
     * @param consumer Callback for location data
     * @throws SecurityException If we're missing location permissions
     */
    public void getLocation(RequestedAccuracy accuracy,
                            List<String> sources,
                            Consumer<LocationData> consumer) throws SecurityException {
        if (!permissionsManager.hasLocationPermissions()) {
            throw new SecurityException("No location permissions");
        }

        if (optimisedProvider.isSupported()) {
            /* Optimised provider will automatically use the location cache if available
             * and return a fresh (< ~30s) location if available */
            log.d("Using optimised location manager");

            List<ProviderRequest> requests = createRequests(sources, optimisedProvider);
            log.d("Requesting location from providers: %s", Arrays.toString(requests.toArray()));

            if (accuracy == RequestedAccuracy.HIGH) {
                callParallelProviders(requests.iterator(), this::chooseBestLocation, consumer);
            } else {
                callSequentialProviders(requests.iterator(), consumer);
            }
        } else {
            /* The legacy provider will directly request location from the hardware,
             * so we've  gotta implement our own cache checks */
            log.d("Using legacy location manager");

            List<ProviderRequest> cachedRequests = createRequests(sources, legacyCachedProvider);
            List<ProviderRequest> highAccuracyRequests = createRequests(sources, legacyHighAccuracyProvider);
            List<ProviderRequest> allRequests = Stream.concat(cachedRequests.stream(), highAccuracyRequests.stream())
                    .collect(Collectors.toList());
            log.d("Requesting location from providers: %s", Arrays.toString(allRequests.toArray()));

            if (accuracy == RequestedAccuracy.HIGH) {
                callParallelProviders(allRequests.iterator(), this::chooseBestLocation, consumer);
            } else {
                callSequentialProviders(allRequests.iterator(), consumer);
            }
        }
    }
}
