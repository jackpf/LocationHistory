package com.jackpf.locationhistory.client.ui;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LocationProviderItem {
    private String providerName;
    private boolean enabled;
}
