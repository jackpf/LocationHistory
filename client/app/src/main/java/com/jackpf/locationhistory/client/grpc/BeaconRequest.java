package com.jackpf.locationhistory.client.grpc;

import android.location.Location;

import java.util.HashMap;
import java.util.Map;

import lombok.Value;

@Value
public class BeaconRequest {
    private long timestamp;
    private double lat;
    private double lon;
    private float accuracy;
    private Map<String, String> metadata;

    public static BeaconRequest fromLocation(Location location) {
        return new BeaconRequest(
                System.currentTimeMillis(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                new HashMap<String, String>() {{
                    put("provider", location.getProvider());
                }}
        );
    }
}
