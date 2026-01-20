package com.jackpf.locationhistory.client.location;

import static org.junit.Assert.assertEquals;
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

        mockGpsLocation = new LocationData(mock(Location.class), LocationManager.GPS_PROVIDER);
        mockNetworkLocation = new LocationData(mock(Location.class), LocationManager.NETWORK_PROVIDER);
    }

    @Test(expected = SecurityException.class)
    public void getLocation_throwsSecurityException_whenNoPermissions() {
        when(permissionsManager.hasLocationPermissions()).thenReturn(false);

        locationService.getLocation(RequestedAccuracy.HIGH, data -> {
        });
    }

    // region Optimised Provider Tests

    @Test
    public void getLocation_usesOptimisedProvider_whenSupported() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        stubProviderToReturn(optimisedProvider, mockGpsLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, result::set);

        verify(optimisedProvider).provide(eq(LocationManager.GPS_PROVIDER), anyInt(), any());
    }

    @Test
    public void getLocation_optimised_highAccuracy_prioritizesGps() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        stubProviderToReturn(optimisedProvider, mockGpsLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, result::set);

        assertEquals(LocationManager.GPS_PROVIDER, result.get().getSource());
    }

    @Test
    public void getLocation_optimised_balanced_prioritizesNetwork() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        stubProviderToReturn(optimisedProvider, mockNetworkLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.BALANCED, result::set);

        assertEquals(LocationManager.NETWORK_PROVIDER, result.get().getSource());
    }

    @Test
    public void getLocation_optimised_fallsBackToNetwork_whenGpsFails() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        stubProviderToReturnForSource(optimisedProvider, LocationManager.GPS_PROVIDER, null);
        stubProviderToReturnForSource(optimisedProvider, LocationManager.NETWORK_PROVIDER, mockNetworkLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, result::set);

        assertEquals(LocationManager.NETWORK_PROVIDER, result.get().getSource());
    }

    // endregion

    // region Legacy Provider Tests

    @Test
    public void getLocation_usesLegacyProviders_whenOptimisedNotSupported() {
        when(optimisedProvider.isSupported()).thenReturn(false);
        stubProviderToReturn(legacyHighAccuracyProvider, mockGpsLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, result::set);

        verify(legacyHighAccuracyProvider).provide(eq(LocationManager.GPS_PROVIDER), anyInt(), any());
        verify(optimisedProvider, never()).provide(any(), anyInt(), any());
    }

    @Test
    public void getLocation_legacy_highAccuracy_prioritizesHighAccuracyGps() {
        when(optimisedProvider.isSupported()).thenReturn(false);
        stubProviderToReturn(legacyHighAccuracyProvider, mockGpsLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, result::set);

        verify(legacyHighAccuracyProvider).provide(eq(LocationManager.GPS_PROVIDER), anyInt(), any());
    }

    @Test
    public void getLocation_legacy_balanced_prioritizesCachedGps() {
        when(optimisedProvider.isSupported()).thenReturn(false);
        stubProviderToReturn(legacyCachedProvider, mockGpsLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.BALANCED, result::set);

        verify(legacyCachedProvider).provide(eq(LocationManager.GPS_PROVIDER), anyInt(), any());
    }

    @Test
    public void getLocation_legacy_fallsBackThroughProviders() {
        when(optimisedProvider.isSupported()).thenReturn(false);
        // All providers return null except the last one
        stubProviderToReturn(legacyHighAccuracyProvider, null);
        stubProviderToReturnForSource(legacyCachedProvider, LocationManager.GPS_PROVIDER, null);
        stubProviderToReturnForSource(legacyCachedProvider, LocationManager.NETWORK_PROVIDER, mockNetworkLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, result::set);

        assertEquals(LocationManager.NETWORK_PROVIDER, result.get().getSource());
    }

    // endregion

    // region Provider Availability Tests

    @Test
    public void getLocation_skipsDisabledProvider() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        when(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(false);
        stubProviderToReturnForSource(optimisedProvider, LocationManager.NETWORK_PROVIDER, mockNetworkLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, result::set);

        verify(optimisedProvider, never()).provide(eq(LocationManager.GPS_PROVIDER), anyInt(), any());
        assertEquals(LocationManager.NETWORK_PROVIDER, result.get().getSource());
    }

    @Test
    public void getLocation_returnsNull_whenAllProvidersFail() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        stubProviderToReturn(optimisedProvider, null);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, result::set);

        assertNull(result.get());
    }

    @Test
    public void getLocation_returnsNull_whenAllProvidersDisabled() {
        when(optimisedProvider.isSupported()).thenReturn(true);
        when(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(false);
        when(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(false);

        AtomicReference<LocationData> result = new AtomicReference<>();
        locationService.getLocation(RequestedAccuracy.HIGH, result::set);

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
