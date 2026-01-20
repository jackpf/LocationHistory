package com.jackpf.locationhistory.client.worker;

import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.SetLocationResponse;
import com.jackpf.locationhistory.client.client.BeaconClientFactory;
import com.jackpf.locationhistory.client.client.ssl.TrustedCertStorage;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.location.LocationData;
import com.jackpf.locationhistory.client.location.LocationService;
import com.jackpf.locationhistory.client.location.RequestedAccuracy;
import com.jackpf.locationhistory.client.model.DeviceState;
import com.jackpf.locationhistory.client.service.DeviceStateService;
import com.jackpf.locationhistory.client.service.LocationUpdateService;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import lombok.Getter;

public class BeaconContext {
    @Getter
    private final BeaconClient client;
    @Getter
    private final DeviceState deviceState;
    private final LocationService locationService;
    private final DeviceStateService deviceStateService;
    private final LocationUpdateService locationUpdateService;

    public BeaconContext(
            ConfigRepository configRepository,
            TrustedCertStorage trustedCertStorage,
            DeviceState deviceState,
            LocationService locationService,
            Executor executor
    ) throws IOException {
        BeaconClientFactory.BeaconClientParams params = new BeaconClientFactory.BeaconClientParams(
                configRepository.getServerHost(),
                configRepository.getServerPort(),
                true,
                BeaconClientFactory.DEFAULT_TIMEOUT
        );
        this.client = BeaconClientFactory.createPooledClient(params, trustedCertStorage);
        this.deviceState = deviceState;
        this.locationService = locationService;
        this.deviceStateService = new DeviceStateService(client, executor);
        this.locationUpdateService = new LocationUpdateService(client, executor);
    }

    public ListenableFuture<DeviceState> onDeviceStateReady() {
        return deviceStateService.onDeviceStateReady(deviceState);
    }

    public void getLocation(RequestedAccuracy accuracy, Consumer<LocationData> consumer) throws SecurityException {
        locationService.getLocation(accuracy, consumer);
    }

    public ListenableFuture<SetLocationResponse> setLocation(LocationData locationData) {
        return locationUpdateService.setLocation(deviceState, locationData);
    }
}