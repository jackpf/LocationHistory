package com.jackpf.locationhistory.client.location;

import android.location.Location;

import lombok.Value;

@Value
public class LocationData {
    private static final String PASSIVE_SOURCE = "passive";
    private static final String PASSIVE_PROVIDER = "passive";

    Location location;
    String source;
    String provider;

    public static LocationData passive(Location location) {
        return new LocationData(location, PASSIVE_SOURCE, PASSIVE_PROVIDER);
    }
}
