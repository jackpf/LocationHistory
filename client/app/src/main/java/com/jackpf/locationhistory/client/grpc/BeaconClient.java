package com.jackpf.locationhistory.client.grpc;

import static com.jackpf.locationhistory.client.grpc.util.GrpcWrapper.executeWrapped;

import com.jackpf.locationhistory.BeaconServiceGrpc;
import com.jackpf.locationhistory.CheckDeviceRequest;
import com.jackpf.locationhistory.CheckDeviceResponse;
import com.jackpf.locationhistory.DeviceStatus;
import com.jackpf.locationhistory.PingRequest;
import com.jackpf.locationhistory.PingResponse;
import com.jackpf.locationhistory.RegisterDeviceRequest;
import com.jackpf.locationhistory.RegisterDeviceResponse;
import com.jackpf.locationhistory.SetLocationRequest;
import com.jackpf.locationhistory.SetLocationResponse;
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

public class BeaconClient implements AutoCloseable {
    private final BeaconServiceGrpc.BeaconServiceBlockingStub beaconService;
    private final ManagedChannel channel;
    private final long timeoutMillis;
    private final Consumer<StatusRuntimeException> failureCallback;

    private final Logger log = new Logger(this);

    public BeaconClient(
            ManagedChannel channel,
            long timeoutMillis,
            Consumer<StatusRuntimeException> failureCallback) {
        this.channel = channel;
        beaconService = BeaconServiceGrpc
                .newBlockingStub(channel);
        this.timeoutMillis = timeoutMillis;
        this.failureCallback = failureCallback;
    }

    private BeaconServiceGrpc.BeaconServiceBlockingStub createStub() {
        return beaconService
                .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public void ping() throws IOException {
        log.d("Ping request");

        PingRequest pingRequest = Requests.pingRequest();
        PingResponse pingResponse = executeWrapped(() ->
                        createStub().ping(pingRequest),
                "Ping request failed",
                this.failureCallback
        );

        log.d("Ping response: %s", pingResponse);

        if (!"pong".equals(pingResponse.getMessage())) {
            throw new IOException(String.format("Invalid ping response: %s", pingResponse.getMessage()));
        }
    }

    public DeviceStatus checkDevice(String deviceId) throws IOException {
        log.d("Check device request");

        CheckDeviceRequest checkDeviceRequest = Requests.checkDeviceRequest(deviceId);
        CheckDeviceResponse checkDeviceResponse = executeWrapped(() ->
                        createStub().checkDevice(checkDeviceRequest),
                "Check device failed",
                this.failureCallback
        );

        log.d("Check device response: %s", checkDeviceResponse);

        return checkDeviceResponse.getStatus();
    }

    public boolean registerDevice(String deviceId, String deviceName, String publicKey) throws IOException {
        log.d("Register device request");

        RegisterDeviceRequest registerDeviceRequest = Requests.registerDeviceRequest(deviceId, deviceName, publicKey);
        RegisterDeviceResponse registerDeviceResponse = executeWrapped(() ->
                        createStub().registerDevice(registerDeviceRequest),
                "Register device failed",
                this.failureCallback
        );

        log.d("Register device response: %s", registerDeviceResponse);

        return registerDeviceResponse.getSuccess();
    }

    public boolean sendLocation(String deviceId, String publicKey, BeaconRequest request) throws IOException {
        log.d("Set location request: %s", request.toString());
        SetLocationRequest setLocationRequest = Requests.setLocationRequest(
                deviceId,
                publicKey,
                request.getLat(),
                request.getLon(),
                (double) request.getAccuracy(),
                request.getTimestamp()
        );
        SetLocationResponse setLocationResponse = executeWrapped(() ->
                        createStub().setLocation(setLocationRequest),
                "Send location failed",
                this.failureCallback
        );
        log.d("Set location response: %s", setLocationResponse);

        return setLocationResponse.getSuccess();
    }

    @Override
    public void close() {
        channel.shutdownNow();
    }
}
