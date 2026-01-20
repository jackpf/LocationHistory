package com.jackpf.locationhistory.client.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.AsyncCallable;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.client.client.ssl.TrustedCertStorage;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.location.LocationData;
import com.jackpf.locationhistory.client.location.LocationService;
import com.jackpf.locationhistory.client.location.RequestedAccuracy;
import com.jackpf.locationhistory.client.model.DeviceState;
import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class BeaconTask {
    private final PermissionsManager permissionsManager;
    private final Callable<BeaconContext> beaconContextFactory;
    private final ExecutorService executor;

    private final Logger log = new Logger(this);

    public BeaconTask(PermissionsManager permissionsManager,
                      Callable<BeaconContext> beaconContextFactory,
                      ExecutorService executor) {
        this.permissionsManager = permissionsManager;
        this.beaconContextFactory = beaconContextFactory;
        this.executor = executor;
    }

    public static BeaconTask create(@NonNull Context context, ExecutorService executor) throws IOException {
        ConfigRepository configRepository = new ConfigRepository(context);
        TrustedCertStorage trustedCertStorage = new TrustedCertStorage(context);
        PermissionsManager permissionsManager = new PermissionsManager(context);
        DeviceState deviceState = DeviceState.fromConfig(configRepository);
        LocationService locationService = LocationService.create(context, executor);

        BeaconContext beaconContext = new BeaconContext(
                configRepository,
                trustedCertStorage,
                deviceState,
                locationService,
                executor
        );

        return new BeaconTask(
                permissionsManager,
                () -> beaconContext,
                executor
        );
    }

    public ListenableFuture<BeaconResult> run() {
        return Futures.submitAsync(() -> {
            if (!permissionsManager.hasLocationPermissions()) {
                return Futures.immediateFailedFuture(new NoLocationPermissionsException());
            }

            BeaconContext beaconContext = beaconContextFactory.call();
            ListenableFuture<BeaconResult> beaconResult = onDeviceReady(beaconContext, () ->
                    requestLocationUpdate(beaconContext, (locationData) ->
                            handleLocationUpdate(beaconContext, locationData, () ->
                                    Futures.immediateFuture(new BeaconResult(beaconContext.getDeviceState(), locationData))
                            )
                    )
            );
            beaconResult.addListener(() -> beaconContext.getDeviceState().storeToConfig(beaconContext.getConfigRepository()),
                    executor
            );
            return beaconResult;
        }, executor);
    }

    private <T> ListenableFuture<T> onDeviceReady(BeaconContext beaconContext, AsyncCallable<T> onReady) {
        log.d("Checking device ready state");

        return Futures.transformAsync(
                beaconContext.onDeviceStateReady(),
                state -> {
                    if (state.isReady()) return onReady.call();
                    else return Futures.immediateFailedFuture(new DeviceNotReadyException());
                },
                executor
        );
    }

    private <T> ListenableFuture<T> requestLocationUpdate(BeaconContext beaconContext, AsyncFunction<LocationData, T> onLocation) {
        log.d("Request location data");

        return Futures.transformAsync(
                CallbackToFutureAdapter.getFuture(completer -> {
                    beaconContext.getLocation(RequestedAccuracy.BALANCED, locationData -> {
                        if (locationData != null) completer.set(locationData);
                        else completer.setException(new EmptyLocationDataException());
                    });
                    return "requestLocationUpdate";
                }),
                onLocation,
                executor
        );
    }

    private <T> ListenableFuture<T> handleLocationUpdate(BeaconContext beaconContext, LocationData locationData, AsyncCallable<T> onSuccess) {
        log.d("Received location data: %s", locationData.toString());

        return Futures.transformAsync(
                beaconContext.setLocation(locationData),
                response -> {
                    if (response.getSuccess()) {
                        beaconContext.getDeviceState().setLastRunTimestamp(System.currentTimeMillis());
                        return onSuccess.call();
                    } else {
                        return Futures.immediateFailedFuture(new SetLocationFailedException());
                    }
                },
                executor
        );
    }

    public static ListenableFuture<BeaconResult> runSafe(Context context, ExecutorService executor) {
        try {
            return BeaconTask.create(context, executor).run();
        } catch (IOException e) {
            return Futures.immediateFailedFuture(e);
        }
    }
}
