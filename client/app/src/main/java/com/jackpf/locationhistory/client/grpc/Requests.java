package com.jackpf.locationhistory.client.grpc;

import com.jackpf.locationhistory.CheckDeviceRequest;
import com.jackpf.locationhistory.Device;
import com.jackpf.locationhistory.Location;
import com.jackpf.locationhistory.PingRequest;
import com.jackpf.locationhistory.PushHandler;
import com.jackpf.locationhistory.RegisterDeviceRequest;
import com.jackpf.locationhistory.RegisterPushHandlerRequest;
import com.jackpf.locationhistory.SetLocationRequest;

import java.util.Map;

public class Requests {
    private static Device device(String deviceId, String deviceName) {
        return Device.newBuilder()
                .setId(deviceId)
                .setName(deviceName)
                .build();
    }

    private static Location location(
            Double lat,
            Double lon,
            Double accuracy,
            Map<String, String> metadata
    ) {
        return Location.newBuilder()
                .setLat(lat)
                .setLon(lon)
                .setAccuracy(accuracy)
                .putAllMetadata(metadata)
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
                .setDeviceId(deviceId)
                .build();
    }

    public static RegisterDeviceRequest registerDeviceRequest(String deviceId, String deviceName) {
        return RegisterDeviceRequest
                .newBuilder()
                .setDevice(device(deviceId, deviceName))
                .build();
    }

    public static SetLocationRequest setLocationRequest(
            String deviceId,
            Double lat,
            Double lon,
            Double accuracy,
            Long timestamp,
            Map<String, String> metadata
    ) {
        return SetLocationRequest
                .newBuilder()
                .setDeviceId(deviceId)
                .setLocation(location(lat, lon, accuracy, metadata))
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

    public static RegisterPushHandlerRequest unregisterPushHandler(String deviceId) {
        return RegisterPushHandlerRequest
                .newBuilder()
                .setDeviceId(deviceId)
                // Empty push handler
                .build();
    }
}
