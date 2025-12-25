package com.jackpf.locationhistory.client.http;

import androidx.annotation.NonNull;

import com.jackpf.locationhistory.client.util.Log;

import java.io.IOException;

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

    public BeaconClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void send(BeaconRequest request) {
        Log.d("Sending location data to server: %s".formatted(request.toString()));

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
