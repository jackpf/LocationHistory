package com.jackpf.locationhistory.client.service;

import static com.jackpf.locationhistory.DeviceStatus.DEVICE_UNKNOWN;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.CheckDeviceResponse;
import com.jackpf.locationhistory.DeviceStatus;
import com.jackpf.locationhistory.RegisterDeviceResponse;
import com.jackpf.locationhistory.client.client.util.GrpcFutureWrapper;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.model.DeviceState;
import com.jackpf.locationhistory.client.util.Logger;

import java.util.concurrent.Executor;

public class DeviceStateService {
    private final BeaconClient beaconClient;
    private final Executor backgroundExecutor;
    private final Logger log = new Logger(this);

    public DeviceStateService(
            BeaconClient beaconClient,
            Executor backgroundExecutor
    ) {
        this.beaconClient = beaconClient;
        this.backgroundExecutor = backgroundExecutor;
    }

    private boolean shouldRegisterDevice(DeviceStatus deviceStatus) {
        return deviceStatus == DEVICE_UNKNOWN;
    }

    private ListenableFuture<RegisterDeviceResponse> registerDevice(DeviceState deviceState) {
        return beaconClient.registerDevice(
                deviceState.getDeviceId(),
                deviceState.getDeviceName(),
                GrpcFutureWrapper.empty()
        );
    }

    public ListenableFuture<DeviceState> onDeviceStateReady(DeviceState deviceState) {
        log.d("Checking device state");

        if (deviceState.isReady()) {
            log.d("Device is already ready");
            return Futures.immediateFuture(deviceState);
        }

        ListenableFuture<CheckDeviceResponse> checkDevice = beaconClient.checkDevice(
                deviceState.getDeviceId(),
                GrpcFutureWrapper.empty()
        );

        ListenableFuture<DeviceState> registerDevice = Futures.transformAsync(checkDevice, checkDeviceResponse -> {
            DeviceStatus status = checkDeviceResponse.getStatus();
            log.d("Device %s has state %s", deviceState.getDeviceId(), status);
            deviceState.updateFromStatus(status);

            if (shouldRegisterDevice(status)) {
                log.d("Registering device");

                return Futures.transform(registerDevice(deviceState),
                        registerDeviceResponse -> deviceState.updateFromStatus(registerDeviceResponse.getStatus()),
                        backgroundExecutor
                );
            } else {
                log.d("Not registering device");
                return Futures.immediateFuture(deviceState);
            }
        }, backgroundExecutor);

        return Futures.catchingAsync(registerDevice, Exception.class, e -> {
            log.e("Register device error", e);
            deviceState.setNotReady();
            return Futures.immediateFailedFuture(e);
        }, backgroundExecutor);
    }
}
