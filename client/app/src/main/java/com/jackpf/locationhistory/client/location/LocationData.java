package com.jackpf.locationhistory.client.location;

import android.location.Location;

import lombok.Value;

@Value
public class LocationData {
    Location location;
    String source;
}
