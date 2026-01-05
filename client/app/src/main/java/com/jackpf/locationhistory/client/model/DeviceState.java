package com.jackpf.locationhistory.client.model;

import java.util.concurrent.atomic.AtomicBoolean;

public class DeviceState {
    private final AtomicBoolean ready = new AtomicBoolean(false);

    public DeviceState setReady() {
        ready.set(true);
        return this;
    }

    public DeviceState setNotReady() {
        ready.set(false);
        return this;
    }

    public boolean isReady() {
        return ready.get();
    }
}
