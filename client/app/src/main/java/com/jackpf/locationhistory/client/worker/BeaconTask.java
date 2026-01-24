package com.jackpf.locationhistory.client.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.AsyncCallable;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
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
import java.util.concurrent.Executor;

public class BeaconTask {
    private final PermissionsManager permissionsManager;
    private final Callable<BeaconContext> beaconContextFactory;
    private final Executor executor;

    private final Logger log = new Logger(this);

    private static final String START_MESSAGE = "Beacon task started";
    private static final String SUCCESS_MESSAGE = "Beacon task completed successfully";
    private static final String FAILED_MESSAGE = "Beacon task failed";
    private static final String RETRY_MESSAGE = "Beacon task failed as retryable";

    public BeaconTask(PermissionsManager permissionsManager,
                      Callable<BeaconContext> beaconContextFactory,
                      Executor executor) {
        this.permissionsManager = permissionsManager;
        this.beaconContextFactory = beaconContextFactory;
        this.executor = executor;
    }

    public static BeaconTask create(@NonNull Context context, Executor executor) throws IOException {
        ConfigRepository configRepository = new ConfigRepository(context);
        TrustedCertStorage trustedCertStorage = new TrustedCertStorage(context);
        PermissionsManager permissionsManager = new PermissionsManager(context);
        DeviceState deviceState = DeviceState.fromConfig(configRepository);
        LocationService locationService = LocationService.create(context, executor);
        Logger.initContext(context); // Enable file logging

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

    private <T> ListenableFuture<T> runInternal(AsyncFunction<BeaconContext, T> beaconTask) {
        log.i(START_MESSAGE);
        log.appendEventToFile(START_MESSAGE);

        return Futures.submitAsync(() -> {
            if (!permissionsManager.hasLocationPermissions()) {
                return Futures.immediateFailedFuture(new NoLocationPermissionsException());
            }

            BeaconContext beaconContext = beaconContextFactory.call();
            ListenableFuture<T> beaconResult = beaconTask.apply(beaconContext);
            beaconResult.addListener(storeDeviceStateListener(beaconContext), executor);
            Futures.addCallback(beaconResult, loggingCallback(), executor);
            return beaconResult;
        }, executor);
    }

    /**
     * Full flow: fetch location & update
     */
    public ListenableFuture<BeaconResult> run() {
        return runInternal(beaconContext -> onDeviceReady(beaconContext, () ->
                requestLocationUpdate(beaconContext, (locationData) ->
                        handleLocationUpdate(beaconContext, locationData, () ->
                                Futures.immediateFuture(new BeaconResult(beaconContext.getDeviceState(), locationData))
                        )
                )
        ));
    }

    /**
     * Handle passive locations
     */
    public ListenableFuture<BeaconResult> passiveRun(LocationData locationData) {
        return runInternal(beaconContext -> handleLocationUpdate(beaconContext, locationData, () ->
                Futures.immediateFuture(new BeaconResult(beaconContext.getDeviceState(), locationData))
        ));
    }

    private Runnable storeDeviceStateListener(BeaconContext beaconContext) {
        return () -> beaconContext.getDeviceState().storeToConfig(beaconContext.getConfigRepository());
    }

    private <T> FutureCallback<T> loggingCallback() {
        return new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                log.i("%s: %s", SUCCESS_MESSAGE, result);
                log.appendEventToFile("%s: %s", SUCCESS_MESSAGE, result);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                if (t instanceof RetryableException) {
                    log.w(RETRY_MESSAGE, t);
                    log.appendEventToFile("%s: %s", RETRY_MESSAGE, t.getMessage());
                } else {
                    log.e(FAILED_MESSAGE, t);
                    log.appendEventToFile("%s: %s", FAILED_MESSAGE, t.getMessage());
                }
            }
        };
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
                    beaconContext.getLocation(getRequestAccuracy(beaconContext), locationData -> {
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

    private RequestedAccuracy getRequestAccuracy(BeaconContext beaconContext) {
        if (beaconContext.inHighAccuracyMode()) return RequestedAccuracy.HIGH;
        else return RequestedAccuracy.BALANCED;
    }

    public static ListenableFuture<BeaconResult> runSafe(Context context, Executor executor) {
        try {
            return BeaconTask.create(context, executor).run();
        } catch (IOException e) {
            return Futures.immediateFailedFuture(e);
        }
    }
}
