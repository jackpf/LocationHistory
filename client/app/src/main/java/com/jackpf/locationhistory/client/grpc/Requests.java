package com.jackpf.locationhistory.client.grpc;

import com.jackpf.locationhistory.CheckDeviceRequest;
import com.jackpf.locationhistory.Device;
import com.jackpf.locationhistory.Location;
import com.jackpf.locationhistory.PingRequest;
import com.jackpf.locationhistory.RegisterDeviceRequest;
import com.jackpf.locationhistory.SetLocationRequest;

public class Requests {
    private static Device device(String deviceId, String publicKey) {
        return Device.newBuilder()
                .setId(deviceId)
                .setPublicKey(publicKey)
                .build();
    }

    private static Location location(
            Double lat,
            Double lon,
            Double accuracy
    ) {
        return Location.newBuilder()
                .setLat(lat)
                .setLon(lon)
                .setAccuracy(accuracy)
                .build();
    }

    public static PingRequest pingRequest() {
        return PingRequest
                .newBuilder()
                .build();
    }

    public static CheckDeviceRequest checkDeviceRequest(String deviceId) {
        return CheckDeviceRequest
                .newBuilder()
                .setDevice(device(deviceId, ""))
                .build();
    }

    public static RegisterDeviceRequest registerDeviceRequest(String deviceId, String publicKey) {
        return RegisterDeviceRequest
                .newBuilder()
                .setDevice(device(deviceId, publicKey))
                .build();
    }

    public static SetLocationRequest setLocationRequest(
            String deviceId,
            String publicKey,
            Double lat,
            Double lon,
            Double accuracy
    ) {
        return SetLocationRequest
                .newBuilder()
                .setDevice(device(deviceId, publicKey))
                .setLocation(location(lat, lon, accuracy))
                .build();
    }
}
