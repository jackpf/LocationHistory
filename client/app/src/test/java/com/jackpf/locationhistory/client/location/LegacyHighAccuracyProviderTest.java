package com.jackpf.locationhistory.client.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;

import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class LegacyHighAccuracyProviderTest {

    private LocationManager locationManager;
    private LegacyHighAccuracyProvider provider;

    @Before
    public void setUp() {
        locationManager = mock(LocationManager.class);
        provider = new LegacyHighAccuracyProvider(locationManager, MoreExecutors.newDirectExecutorService());
    }

    @Test
    public void provide_returnsLocationWhenReceivedBeforeTimeout() {
        Location mockLocation = mock(Location.class);
        AtomicReference<LocationListener> capturedListener = new AtomicReference<>();

        doAnswer(invocation -> {
            capturedListener.set(invocation.getArgument(1));
            return null;
        }).when(locationManager).requestSingleUpdate(
                eq(LocationManager.GPS_PROVIDER),
                any(LocationListener.class),
                any(Looper.class)
        );

        AtomicReference<LocationData> result = new AtomicReference<>();
        provider.provide(LocationManager.GPS_PROVIDER, 5000, result::set);

        // Simulate location received
        assertNotNull(capturedListener.get());
        capturedListener.get().onLocationChanged(mockLocation);

        assertNotNull(result.get());
        assertEquals(mockLocation, result.get().getLocation());
        assertEquals(LocationManager.GPS_PROVIDER, result.get().getSource());
    }

    @Test
    public void provide_returnsNullOnTimeout() {
        doAnswer(invocation -> null).when(locationManager).requestSingleUpdate(
                eq(LocationManager.GPS_PROVIDER),
                any(LocationListener.class),
                any(Looper.class)
        );

        AtomicReference<LocationData> result = new AtomicReference<>();
        provider.provide(LocationManager.GPS_PROVIDER, 1000, result::set);

        // Fast-forward time to trigger timeout
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertNull(result.get());
    }

    @Test
    public void provide_removesUpdatesAfterLocationReceived() {
        Location mockLocation = mock(Location.class);
        AtomicReference<LocationListener> capturedListener = new AtomicReference<>();

        doAnswer(invocation -> {
            capturedListener.set(invocation.getArgument(1));
            return null;
        }).when(locationManager).requestSingleUpdate(
                eq(LocationManager.GPS_PROVIDER),
                any(LocationListener.class),
                any(Looper.class)
        );

        provider.provide(LocationManager.GPS_PROVIDER, 5000, data -> {
        });

        capturedListener.get().onLocationChanged(mockLocation);

        verify(locationManager).removeUpdates(capturedListener.get());
    }

    @Test
    public void provide_removesUpdatesOnTimeout() {
        AtomicReference<LocationListener> capturedListener = new AtomicReference<>();

        doAnswer(invocation -> {
            capturedListener.set(invocation.getArgument(1));
            return null;
        }).when(locationManager).requestSingleUpdate(
                eq(LocationManager.GPS_PROVIDER),
                any(LocationListener.class),
                any(Looper.class)
        );

        provider.provide(LocationManager.GPS_PROVIDER, 1000, data -> {
        });

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(locationManager).removeUpdates(capturedListener.get());
    }

    @Test
    public void provide_ignoresSecondLocationUpdate() {
        Location mockLocation1 = mock(Location.class);
        Location mockLocation2 = mock(Location.class);
        AtomicReference<LocationListener> capturedListener = new AtomicReference<>();

        doAnswer(invocation -> {
            capturedListener.set(invocation.getArgument(1));
            return null;
        }).when(locationManager).requestSingleUpdate(
                eq(LocationManager.GPS_PROVIDER),
                any(LocationListener.class),
                any(Looper.class)
        );

        AtomicReference<LocationData> result = new AtomicReference<>();
        provider.provide(LocationManager.GPS_PROVIDER, 5000, result::set);

        // First location
        capturedListener.get().onLocationChanged(mockLocation1);
        // Second location (should be ignored)
        capturedListener.get().onLocationChanged(mockLocation2);

        assertEquals(mockLocation1, result.get().getLocation());
    }

    @Test
    public void provide_ignoresTimeoutAfterLocationReceived() {
        Location mockLocation = mock(Location.class);
        AtomicReference<LocationListener> capturedListener = new AtomicReference<>();

        doAnswer(invocation -> {
            capturedListener.set(invocation.getArgument(1));
            return null;
        }).when(locationManager).requestSingleUpdate(
                eq(LocationManager.GPS_PROVIDER),
                any(LocationListener.class),
                any(Looper.class)
        );

        AtomicReference<LocationData> result = new AtomicReference<>();
        provider.provide(LocationManager.GPS_PROVIDER, 1000, result::set);

        // Location received before timeout
        capturedListener.get().onLocationChanged(mockLocation);

        // Now trigger timeout (should be ignored)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Result should still be the location, not null
        assertNotNull(result.get());
        assertEquals(mockLocation, result.get().getLocation());
    }
}
