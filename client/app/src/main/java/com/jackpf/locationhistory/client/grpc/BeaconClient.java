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
import com.jackpf.locationhistory.client.util.Log;

import java.io.IOException;

import io.grpc.ManagedChannel;

public class BeaconClient {
    private final BeaconServiceGrpc.BeaconServiceBlockingStub beaconService;

    public BeaconClient(ManagedChannel channel) {
        // TODO Make non-blocking
        beaconService = BeaconServiceGrpc.newBlockingStub(channel);
    }

    public void ping() throws IOException {
        Log.d("Ping request");

        PingRequest pingRequest = Requests.pingRequest();
        PingResponse pingResponse = executeWrapped(() ->
                        beaconService.ping(pingRequest),
                "Ping request failed"
        );

        Log.d("Ping response: %s".formatted(pingResponse));

        if (!"pong".equals(pingResponse.getMessage())) {
            throw new IOException("Invalid ping response: %s".formatted(pingResponse.getMessage()));
        }
    }

    public DeviceStatus checkDevice(String deviceId) throws IOException {
        Log.d("Check device request");

        CheckDeviceRequest checkDeviceRequest = Requests.checkDeviceRequest(deviceId);
        CheckDeviceResponse checkDeviceResponse = executeWrapped(() ->
                        beaconService.checkDevice(checkDeviceRequest),
                "Check device failed"
        );

        Log.d("Check device response: %s".formatted(checkDeviceResponse));

        return checkDeviceResponse.getStatus();
    }

    public boolean registerDevice(String deviceId, String publicKey) throws IOException {
        Log.d("Register device request");

        RegisterDeviceRequest registerDeviceRequest = Requests.registerDeviceRequest(deviceId, publicKey);
        RegisterDeviceResponse registerDeviceResponse = executeWrapped(() ->
                        beaconService.registerDevice(registerDeviceRequest),
                "Register device failed"
        );

        Log.d("Register device response: %s".formatted(registerDeviceResponse));

        return registerDeviceResponse.getSuccess();
    }

    public boolean sendLocation(String deviceId, String publicKey, BeaconRequest request) throws IOException {
        Log.d("Sending location request: %s".formatted(request.toString()));

        DeviceStatus deviceStatus = checkDevice(deviceId);
        if (deviceStatus != DeviceStatus.DEVICE_REGISTERED) {
            Log.w("Device %s not registered, not sending location".formatted(deviceId));
            return false;
        }

        SetLocationRequest setLocationRequest = Requests.setLocationRequest(
                deviceId,
                publicKey,
                request.getLat(),
                request.getLon(),
                (double) request.getAccuracy()
        );
        SetLocationResponse setLocationResponse = executeWrapped(() ->
                        beaconService.setLocation(setLocationRequest),
                "Send location failed"
        );

        Log.d("Set location response: %s".formatted(setLocationResponse));

        return setLocationResponse.getSuccess();
    }
}
