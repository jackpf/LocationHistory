package com.jackpf.locationhistory.client.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.location.Location;
import android.location.LocationManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class LegacyCachedProviderTest {

    private LocationManager locationManager;
    private ExecutorService executorService;
    private LegacyCachedProvider provider;

    @Before
    public void setUp() {
        locationManager = mock(LocationManager.class);
        // Use a direct executor for testing - runs tasks immediately on same thread
        executorService = mock(ExecutorService.class);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        provider = new LegacyCachedProvider(locationManager, executorService);
    }

    @Test
    public void provide_returnsFreshCachedLocation() {
        Location mockLocation = mock(Location.class);
        when(mockLocation.getTime()).thenReturn(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1));
        when(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)).thenReturn(mockLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        provider.provide(LocationManager.GPS_PROVIDER, 5000, result::set);

        assertNotNull(result.get());
        assertEquals(mockLocation, result.get().getLocation());
        assertEquals(LocationManager.GPS_PROVIDER, result.get().getSource());
    }

    @Test
    public void provide_returnsNullForStaleLocation() {
        Location mockLocation = mock(Location.class);
        // Location is 10 minutes old (stale, threshold is 5 minutes)
        when(mockLocation.getTime()).thenReturn(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10));
        when(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)).thenReturn(mockLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        provider.provide(LocationManager.GPS_PROVIDER, 5000, result::set);

        assertNull(result.get());
    }

    @Test
    public void provide_returnsNullWhenNoCachedLocation() {
        when(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)).thenReturn(null);

        AtomicReference<LocationData> result = new AtomicReference<>();
        provider.provide(LocationManager.GPS_PROVIDER, 5000, result::set);

        assertNull(result.get());
    }

    @Test
    public void provide_returnsLocationAtFreshnessThresholdBoundary() {
        Location mockLocation = mock(Location.class);
        // Location is exactly at the 5 minute threshold (should be considered stale)
        when(mockLocation.getTime()).thenReturn(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5));
        when(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)).thenReturn(mockLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        provider.provide(LocationManager.GPS_PROVIDER, 5000, result::set);

        assertNull(result.get());
    }

    @Test
    public void provide_returnsLocationJustUnderThreshold() {
        Location mockLocation = mock(Location.class);
        // Location is just under 5 minutes old (should be fresh)
        when(mockLocation.getTime()).thenReturn(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5) + 1000);
        when(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)).thenReturn(mockLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        provider.provide(LocationManager.GPS_PROVIDER, 5000, result::set);

        assertNotNull(result.get());
        assertEquals(mockLocation, result.get().getLocation());
    }

    @Test
    public void provide_usesCorrectSource() {
        Location mockLocation = mock(Location.class);
        when(mockLocation.getTime()).thenReturn(System.currentTimeMillis());
        when(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)).thenReturn(mockLocation);

        AtomicReference<LocationData> result = new AtomicReference<>();
        provider.provide(LocationManager.NETWORK_PROVIDER, 5000, result::set);

        assertNotNull(result.get());
        assertEquals(LocationManager.NETWORK_PROVIDER, result.get().getSource());
    }

    @Test
    public void provide_callsConsumerViaExecutor_whenFresh() {
        Location mockLocation = mock(Location.class);
        when(mockLocation.getTime()).thenReturn(System.currentTimeMillis());
        when(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)).thenReturn(mockLocation);

        provider.provide(LocationManager.GPS_PROVIDER, 5000, data -> {});

        verify(executorService).execute(any(Runnable.class));
    }

    @Test
    public void provide_callsConsumerViaExecutor_whenStale() {
        Location mockLocation = mock(Location.class);
        when(mockLocation.getTime()).thenReturn(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10));
        when(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)).thenReturn(mockLocation);

        provider.provide(LocationManager.GPS_PROVIDER, 5000, data -> {});

        verify(executorService).execute(any(Runnable.class));
    }
}
