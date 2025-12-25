package com.jackpf.locationhistory.client.http;

import android.location.Location;

import lombok.Value;

@Value
public class BeaconRequest {
    long timestamp;
    double lat;
    double lon;
    float accuracy;

    public static BeaconRequest fromLocation(Location location) {
        return new BeaconRequest(
                System.currentTimeMillis(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy()
        );
    }
}
