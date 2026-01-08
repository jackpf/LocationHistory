package com.jackpf.locationhistory.client.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.SetLocationResponse;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.grpc.BeaconRequest;
import com.jackpf.locationhistory.client.grpc.util.GrpcFutureWrapper;
import com.jackpf.locationhistory.client.location.LocationProvider;
import com.jackpf.locationhistory.client.model.DeviceState;
import com.jackpf.locationhistory.client.util.Logger;

import java.util.concurrent.Executor;

public class LocationUpdateService {
    private final BeaconClient beaconClient;
    private final Executor backgroundExecutor;
    private final Logger log = new Logger(this);

    public LocationUpdateService(
            BeaconClient beaconClient,
            Executor backgroundExecutor
    ) {
        this.beaconClient = beaconClient;
        this.backgroundExecutor = backgroundExecutor;
    }

    public ListenableFuture<SetLocationResponse> setLocation(DeviceState deviceState, LocationProvider.LocationData locationData) {
        ListenableFuture<SetLocationResponse> setLocation = beaconClient.sendLocation(
                deviceState.getDeviceId(),
                BeaconRequest.fromLocation(locationData.getLocation()),
                GrpcFutureWrapper.empty()
        );

        return Futures.catchingAsync(setLocation, Exception.class, e -> {
            log.e("Set location error", e);
            deviceState.setNotReady();
            return Futures.immediateFailedFuture(e);
        }, backgroundExecutor);
    }
}
