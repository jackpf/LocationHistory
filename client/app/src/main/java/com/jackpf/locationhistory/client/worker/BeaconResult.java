package com.jackpf.locationhistory.client.worker;

import com.jackpf.locationhistory.client.location.LocationData;
import com.jackpf.locationhistory.client.model.DeviceState;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BeaconResult {
    DeviceState deviceState;
    LocationData locationData;
}
