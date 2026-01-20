package com.jackpf.locationhistory.client.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.CancellationSignal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@RunWith(RobolectricTestRunner.class)
public class OptimisedProviderTest {

    private LocationManager locationManager;
    private ExecutorService executorService;
    private OptimisedProvider provider;

    @Before
    public void setUp() {
        locationManager = mock(LocationManager.class);
        executorService = Executors.newSingleThreadExecutor();
        provider = new OptimisedProvider(locationManager, executorService);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void isSupported_returnsTrueOnApiR() {
        assertTrue(provider.isSupported());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void isSupported_returnsFalseOnApiQ() {
        assertFalse(provider.isSupported());
    }

    @Test(expected = RuntimeException.class)
    @Config(sdk = Build.VERSION_CODES.Q)
    public void provide_throwsOnUnsupportedApi() {
        provider.provide(LocationManager.GPS_PROVIDER, 5000, data -> {
        });
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void provide_returnsLocationWhenReceived() {
        Location mockLocation = mock(Location.class);

        doAnswer(invocation -> {
            Consumer<Location> consumer = invocation.getArgument(3);
            consumer.accept(mockLocation);
            return null;
        }).when(locationManager).getCurrentLocation(
                eq(LocationManager.GPS_PROVIDER),
                any(CancellationSignal.class),
                any(Executor.class),
                any(Consumer.class)
        );

        AtomicReference<LocationData> result = new AtomicReference<>();
        provider.provide(LocationManager.GPS_PROVIDER, 5000, result::set);

        assertNotNull(result.get());
        assertEquals(mockLocation, result.get().getLocation());
        assertEquals(LocationManager.GPS_PROVIDER, result.get().getSource());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void provide_returnsNullWhenLocationIsNull() {
        doAnswer(invocation -> {
            Consumer<Location> consumer = invocation.getArgument(3);
            consumer.accept(null);
            return null;
        }).when(locationManager).getCurrentLocation(
                eq(LocationManager.GPS_PROVIDER),
                any(CancellationSignal.class),
                any(Executor.class),
                any(Consumer.class)
        );

        AtomicReference<LocationData> result = new AtomicReference<>();
        provider.provide(LocationManager.GPS_PROVIDER, 5000, result::set);

        assertNull(result.get());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void provide_usesCorrectSource() {
        Location mockLocation = mock(Location.class);

        doAnswer(invocation -> {
            Consumer<Location> consumer = invocation.getArgument(3);
            consumer.accept(mockLocation);
            return null;
        }).when(locationManager).getCurrentLocation(
                eq(LocationManager.NETWORK_PROVIDER),
                any(CancellationSignal.class),
                any(Executor.class),
                any(Consumer.class)
        );

        AtomicReference<LocationData> result = new AtomicReference<>();
        provider.provide(LocationManager.NETWORK_PROVIDER, 5000, result::set);

        assertNotNull(result.get());
        assertEquals(LocationManager.NETWORK_PROVIDER, result.get().getSource());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void provide_passesExecutorToLocationManager() {
        AtomicReference<Executor> capturedExecutor = new AtomicReference<>();

        doAnswer(invocation -> {
            capturedExecutor.set(invocation.getArgument(2));
            Consumer<Location> consumer = invocation.getArgument(3);
            consumer.accept(mock(Location.class));
            return null;
        }).when(locationManager).getCurrentLocation(
                any(String.class),
                any(CancellationSignal.class),
                any(Executor.class),
                any(Consumer.class)
        );

        provider.provide(LocationManager.GPS_PROVIDER, 5000, data -> {
        });

        assertEquals(executorService, capturedExecutor.get());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void provide_callsConsumerWithNullOnTimeout() {
        // Don't call the consumer immediately - simulate slow/no response
        doNothing().when(locationManager).getCurrentLocation(
                any(String.class),
                any(CancellationSignal.class),
                any(Executor.class),
                any(Consumer.class)
        );

        AtomicBoolean consumerCalled = new AtomicBoolean(false);
        AtomicReference<LocationData> result = new AtomicReference<>();

        provider.provide(LocationManager.GPS_PROVIDER, 5000, data -> {
            consumerCalled.set(true);
            result.set(data);
        });

        // Consumer should not be called yet
        assertFalse(consumerCalled.get());

        // Advance time past the timeout
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Now consumer should have been called with null
        assertTrue(consumerCalled.get());
        assertNull(result.get());
    }
}