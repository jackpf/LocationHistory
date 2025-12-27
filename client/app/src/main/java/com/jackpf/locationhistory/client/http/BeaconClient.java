package com.jackpf.locationhistory.client.http;

import androidx.annotation.NonNull;

import com.jackpf.locationhistory.client.util.Log;

import java.io.IOException;

import beacon.java.BeaconServiceGrpc;
import beacon.java.BeaconServiceOuterClass.CheckDeviceRequest;
import beacon.java.BeaconServiceOuterClass.CheckDeviceResponse;
import beacon.java.BeaconServiceOuterClass.Device;
import beacon.java.BeaconServiceOuterClass.RegisterDeviceRequest;
import beacon.java.BeaconServiceOuterClass.RegisterDeviceResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import tools.jackson.databind.ObjectMapper;

public class BeaconClient {
    private final ObjectMapper objectMapper;
    private final BeaconServiceGrpc.BeaconServiceBlockingStub beaconService;

    public BeaconClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("10.0.2.2", 8080)
                .usePlaintext()
                .build();

        // TODO Make non-blocking
        beaconService = BeaconServiceGrpc.newBlockingStub(channel);
    }

    public void send(BeaconRequest request) {
        Log.d("Sending location data to server: %s".formatted(request.toString()));

        String deviceId = "123";
        Device device = Device.newBuilder().setId(deviceId).build();
        CheckDeviceResponse checkDeviceResponse = beaconService.checkDevice(
                CheckDeviceRequest.newBuilder().setDevice(device).build()
        );
        Log.i("Check device response: %s".formatted(checkDeviceResponse.getStatus()));
        RegisterDeviceResponse registerDeviceResponse = beaconService.registerDevice(
                RegisterDeviceRequest.newBuilder().setDevice(device).build()
        );
        Log.i("Register device response: %s".formatted(registerDeviceResponse.getSuccess()));

        String json = objectMapper.writeValueAsString(request);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

        Request req = new Request.Builder()
                .url("https://100.x.y.z:8443/location")
                .addHeader("Authorization", "Bearer YOURTOKEN")
                .post(body)
                .build();

        new OkHttpClient().newCall(req).enqueue(new Callback() {
            public void onFailure(@NonNull Call c, @NonNull IOException e) {
                Log.e("Failed to send to server", e);
            }

            public void onResponse(@NonNull Call c, @NonNull Response r) {
                Log.d("Successful response from server");
                r.close();
            }
        });
    }
}
