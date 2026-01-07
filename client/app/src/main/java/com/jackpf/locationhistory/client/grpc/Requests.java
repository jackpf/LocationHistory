package com.jackpf.locationhistory.client.grpc;

import com.jackpf.locationhistory.CheckDeviceRequest;
import com.jackpf.locationhistory.Device;
import com.jackpf.locationhistory.Location;
import com.jackpf.locationhistory.PingRequest;
import com.jackpf.locationhistory.PushHandler;
import com.jackpf.locationhistory.RegisterDeviceRequest;
import com.jackpf.locationhistory.RegisterPushHandlerRequest;
import com.jackpf.locationhistory.SetLocationRequest;

public class Requests {
    private static Device device(String deviceId, String deviceName, String publicKey) {
        return Device.newBuilder()
                .setId(deviceId)
                .setName(deviceName)
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
                .setDevice(device(deviceId, "", ""))
                .build();
    }

    public static RegisterDeviceRequest registerDeviceRequest(String deviceId, String deviceName, String publicKey) {
        return RegisterDeviceRequest
                .newBuilder()
                .setDevice(device(deviceId, deviceName, publicKey))
                .build();
    }

    public static SetLocationRequest setLocationRequest(
            String deviceId,
            String publicKey,
            Double lat,
            Double lon,
            Double accuracy,
            Long timestamp
    ) {
        return SetLocationRequest
                .newBuilder()
                .setDevice(device(deviceId, "", publicKey))
                .setLocation(location(lat, lon, accuracy))
                .setTimestamp(timestamp)
                .build();
    }

    public static RegisterPushHandlerRequest registerPushHandler(String deviceId, String pushHandlerName, String pushHandlerUrl) {
        return RegisterPushHandlerRequest
                .newBuilder()
                .setDeviceId(deviceId)
                .setPushHandler(
                        PushHandler.newBuilder()
                                .setName(pushHandlerName)
                                .setUrl(pushHandlerUrl)
                                .build()
                )
                .build();
    }
}
