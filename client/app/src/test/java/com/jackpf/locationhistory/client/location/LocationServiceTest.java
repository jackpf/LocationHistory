package com.jackpf.locationhistory.client.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.location.Location;
import android.location.LocationManager;

import com.jackpf.locationhistory.client.permissions.PermissionsManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@RunWith(RobolectricTestRunner.class)
public class LocationServiceTest {

    private LocationManager locationManager;
    private PermissionsManager permissionsManager;
    private LegacyHighAccuracyProvider legacyHighAccuracyProvider;
    private LegacyCachedProvider legacyCachedProvider;
    private OptimisedProvider optimisedProvider;
    private LocationService locationService;

    private LocationData mockGpsLocation;
    private LocationData mockNetworkLocation;

    private static final List<String> DEFAULT_SOURCES = Arrays.asList(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
    );

    private static final List<String> NETWORK_FIRST_SOURCES = Arrays.asList(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER
    );

    @Before
    public void setUp() {
        locationManager = mock(LocationManager.class);
        permissionsManager = mock(PermissionsManager.class);
        legacyHighAccuracyProvider = mock(LegacyHighAccuracyProvider.class);
        legacyCachedProvider = mock(LegacyCachedProvider.class);
        optimisedProvider = mock(OptimisedProvider.class);

        when(permissionsManager.hasLocationPermissions()).thenReturn(true);
        when(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        when(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true);

        locationService = new LocationService(
                locationManager,
                permissionsManager,
                legacyHighAccuracyProvider,
                legacyCachedProvider,
                optimisedProvider
        );

        // GPS location with better accuracy (lower value = more accurate)
        Location gpsLocation = mock(Location.class);
        when(gpsLocation.getAccuracy()).thenReturn(5.0f);
        mockGpsLocation = new LocationData(gpsLocation, LocationManager.GPS_PROVIDER, "mock-provider");

        // Network location with worse accuracy
        Location networkLocation = mock(Location.class);
        when(networkLocation.getAccuracy()).thenReturn(50.0f);
        mockNetworkLocation = new LocationData(networkLocation, LocationManager.NETWORK_PROVIDER, "mock-provider");
    }

    @Test(expected = SecurityException.class)
    public void getLocation_throwsSecurityException_whenNoPermissions() {
        when(permissionsManager.hasLocationPermissions()).thenReturn(false);

        locationService.getLocation(RequestedAccuracy.HIGH, DEFAULT_SOURCES, data -> {
        });
    }

    // region Optimised Provider Tests (HIGH accuracy = parallel, BALANCED = sequential)

    @Test
    public void getLocation_usesOptimisedProvider_whenSupported() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        stubProviderToReturnForSource(optimisedProvider, LocationManager.GPS_PROVIDER, mockGpsLocation);
        stubProviderToReturnForSource(optimisedProvider, LocationManager.NETWORK_PROVIDER, mockNetworkLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, DEFAULT_SOURCES, result::set);

        verify(optimisedProvider).provide(eq(LocationManager.GPS_PROVIDER), anyInt(), any());
        verify(optimisedProvider).provide(eq(LocationManager.NETWORK_PROVIDER), anyInt(), any());
    }

    @Test
    public void getLocation_optimised_highAccuracy_callsBothProvidersInParallel() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        stubProviderToReturnForSource(optimisedProvider, LocationManager.GPS_PROVIDER, mockGpsLocation);
        stubProviderToReturnForSource(optimisedProvider, LocationManager.NETWORK_PROVIDER, mockNetworkLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, DEFAULT_SOURCES, result::set);

        // Both providers should be called for HIGH accuracy (parallel)
        verify(optimisedProvider).provide(eq(LocationManager.GPS_PROVIDER), anyInt(), any());
        verify(optimisedProvider).provide(eq(LocationManager.NETWORK_PROVIDER), anyInt(), any());
    }

    @Test
    public void getLocation_optimised_highAccuracy_selectsBestAccuracy() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        stubProviderToReturnForSource(optimisedProvider, LocationManager.GPS_PROVIDER, mockGpsLocation);
        stubProviderToReturnForSource(optimisedProvider, LocationManager.NETWORK_PROVIDER, mockNetworkLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, DEFAULT_SOURCES, result::set);

        // GPS has better accuracy (5.0f vs 50.0f), so it should be selected
        assertNotNull(result.get());
        assertEquals(LocationManager.GPS_PROVIDER, result.get().getSource());
    }

    @Test
    public void getLocation_optimised_highAccuracy_selectsNetworkWhenMoreAccurate() {
        when(optimisedProvider.isSupported()).thenReturn(true);

        // Make network more accurate than GPS for this test
        Location accurateNetworkLocation = mock(Location.class);
        when(accurateNetworkLocation.getAccuracy()).thenReturn(3.0f);
        LocationData accurateNetwork = new LocationData(accurateNetworkLocation, LocationManager.NETWORK_PROVIDER, "mock");

        stubProviderToReturnForSource(optimisedProvider, LocationManager.GPS_PROVIDER, mockGpsLocation); // 5.0f accuracy
        stubProviderToReturnForSource(optimisedProvider, LocationManager.NETWORK_PROVIDER, accurateNetwork); // 3.0f accuracy

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, DEFAULT_SOURCES, result::set);

        // Network has better accuracy, so it should be selected
        assertEquals(LocationManager.NETWORK_PROVIDER, result.get().getSource());
    }

    @Test
    public void getLocation_optimised_balanced_callsSequentially() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        stubProviderToReturnForSource(optimisedProvider, LocationManager.NETWORK_PROVIDER, mockNetworkLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.BALANCED, NETWORK_FIRST_SOURCES, result::set);

        // Network is first in sources, returns successfully, so GPS shouldn't be called
        verify(optimisedProvider).provide(eq(LocationManager.NETWORK_PROVIDER), anyInt(), any());
        verify(optimisedProvider, never()).provide(eq(LocationManager.GPS_PROVIDER), anyInt(), any());
        assertEquals(LocationManager.NETWORK_PROVIDER, result.get().getSource());
    }

    @Test
    public void getLocation_optimised_balanced_fallsBackToGps_whenNetworkFails() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        stubProviderToReturnForSource(optimisedProvider, LocationManager.NETWORK_PROVIDER, null);
        stubProviderToReturnForSource(optimisedProvider, LocationManager.GPS_PROVIDER, mockGpsLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.BALANCED, DEFAULT_SOURCES, result::set);

        assertEquals(LocationManager.GPS_PROVIDER, result.get().getSource());
    }

    @Test
    public void getLocation_optimised_highAccuracy_returnsNonNullWhenOneProviderFails() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        stubProviderToReturnForSource(optimisedProvider, LocationManager.GPS_PROVIDER, null);
        stubProviderToReturnForSource(optimisedProvider, LocationManager.NETWORK_PROVIDER, mockNetworkLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, DEFAULT_SOURCES, result::set);

        // Even though GPS failed, network succeeded
        assertNotNull(result.get());
        assertEquals(LocationManager.NETWORK_PROVIDER, result.get().getSource());
    }

    // endregion

    // region Legacy Provider Tests

    @Test
    public void getLocation_usesLegacyProviders_whenOptimisedNotSupported() {
        when(optimisedProvider.isSupported()).thenReturn(false);
        stubProviderToReturn(legacyHighAccuracyProvider, mockGpsLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, DEFAULT_SOURCES, result::set);

        verify(legacyHighAccuracyProvider).provide(eq(LocationManager.GPS_PROVIDER), anyInt(), any());
        verify(optimisedProvider, never()).provide(any(), anyInt(), any());
    }

    @Test
    public void getLocation_legacy_highAccuracy_callsAllProvidersInParallel() {
        when(optimisedProvider.isSupported()).thenReturn(false);
        stubProviderToReturnForSource(legacyHighAccuracyProvider, LocationManager.GPS_PROVIDER, mockGpsLocation);
        stubProviderToReturnForSource(legacyHighAccuracyProvider, LocationManager.NETWORK_PROVIDER, mockNetworkLocation);
        stubProviderToReturnForSource(legacyCachedProvider, LocationManager.GPS_PROVIDER, null);
        stubProviderToReturnForSource(legacyCachedProvider, LocationManager.NETWORK_PROVIDER, null);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, DEFAULT_SOURCES, result::set);

        // All 4 providers should be called in parallel for HIGH accuracy
        verify(legacyHighAccuracyProvider).provide(eq(LocationManager.GPS_PROVIDER), anyInt(), any());
        verify(legacyHighAccuracyProvider).provide(eq(LocationManager.NETWORK_PROVIDER), anyInt(), any());
        verify(legacyCachedProvider).provide(eq(LocationManager.GPS_PROVIDER), anyInt(), any());
        verify(legacyCachedProvider).provide(eq(LocationManager.NETWORK_PROVIDER), anyInt(), any());
    }

    @Test
    public void getLocation_legacy_highAccuracy_selectsBestAccuracy() {
        when(optimisedProvider.isSupported()).thenReturn(false);
        stubProviderToReturnForSource(legacyHighAccuracyProvider, LocationManager.GPS_PROVIDER, mockGpsLocation);
        stubProviderToReturnForSource(legacyHighAccuracyProvider, LocationManager.NETWORK_PROVIDER, mockNetworkLocation);
        stubProviderToReturnForSource(legacyCachedProvider, LocationManager.GPS_PROVIDER, null);
        stubProviderToReturnForSource(legacyCachedProvider, LocationManager.NETWORK_PROVIDER, null);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, DEFAULT_SOURCES, result::set);

        // GPS has better accuracy, should be selected
        assertNotNull(result.get());
        assertEquals(LocationManager.GPS_PROVIDER, result.get().getSource());
    }

    @Test
    public void getLocation_legacy_balanced_prioritizesCachedGps() {
        when(optimisedProvider.isSupported()).thenReturn(false);
        stubProviderToReturnForSource(legacyCachedProvider, LocationManager.GPS_PROVIDER, mockGpsLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.BALANCED, DEFAULT_SOURCES, result::set);

        verify(legacyCachedProvider).provide(eq(LocationManager.GPS_PROVIDER), anyInt(), any());
        // High accuracy provider shouldn't be called since cached succeeded
        verify(legacyHighAccuracyProvider, never()).provide(any(), anyInt(), any());
    }

    @Test
    public void getLocation_legacy_balanced_fallsBackThroughProviders() {
        when(optimisedProvider.isSupported()).thenReturn(false);
        // All cached providers return null
        stubProviderToReturnForSource(legacyCachedProvider, LocationManager.GPS_PROVIDER, null);
        stubProviderToReturnForSource(legacyCachedProvider, LocationManager.NETWORK_PROVIDER, null);
        // High accuracy GPS also returns null
        stubProviderToReturnForSource(legacyHighAccuracyProvider, LocationManager.GPS_PROVIDER, null);
        // High accuracy network returns a result
        stubProviderToReturnForSource(legacyHighAccuracyProvider, LocationManager.NETWORK_PROVIDER, mockNetworkLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.BALANCED, DEFAULT_SOURCES, result::set);

        assertEquals(LocationManager.NETWORK_PROVIDER, result.get().getSource());
    }

    // endregion

    // region Provider Availability Tests

    @Test
    public void getLocation_skipsDisabledProvider_highAccuracy() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        when(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(false);
        stubProviderToReturnForSource(optimisedProvider, LocationManager.NETWORK_PROVIDER, mockNetworkLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, DEFAULT_SOURCES, result::set);

        verify(optimisedProvider, never()).provide(eq(LocationManager.GPS_PROVIDER), anyInt(), any());
        assertEquals(LocationManager.NETWORK_PROVIDER, result.get().getSource());
    }

    @Test
    public void getLocation_skipsDisabledProvider_balanced() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        when(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(false);
        stubProviderToReturnForSource(optimisedProvider, LocationManager.GPS_PROVIDER, mockGpsLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.BALANCED, DEFAULT_SOURCES, result::set);

        verify(optimisedProvider, never()).provide(eq(LocationManager.NETWORK_PROVIDER), anyInt(), any());
        assertEquals(LocationManager.GPS_PROVIDER, result.get().getSource());
    }

    @Test
    public void getLocation_returnsNull_whenAllProvidersFail() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        stubProviderToReturn(optimisedProvider, null);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, DEFAULT_SOURCES, result::set);

        assertNull(result.get());
    }

    @Test
    public void getLocation_returnsNull_whenAllProvidersDisabled() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        when(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(false);
        when(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(false);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, DEFAULT_SOURCES, result::set);

        assertNull(result.get());
    }

    // endregion

    // region Helper Methods

    private void stubProviderToReturn(LocationProvider provider, LocationData data) {
        doAnswer(invocation -> {
            Consumer<LocationData> consumer = invocation.getArgument(2);
            consumer.accept(data);
            return null;
        }).when(provider).provide(any(), anyInt(), any());
    }

    private void stubProviderToReturnForSource(LocationProvider provider, String source, LocationData data) {
        doAnswer(invocation -> {
            Consumer<LocationData> consumer = invocation.getArgument(2);
            consumer.accept(data);
            return null;
        }).when(provider).provide(eq(source), anyInt(), any());
    }

    // endregion
}
