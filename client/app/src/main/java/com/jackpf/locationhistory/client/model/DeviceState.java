package com.jackpf.locationhistory.client.model;

import java.util.concurrent.atomic.AtomicBoolean;

public class DeviceState {
    private final AtomicBoolean ready = new AtomicBoolean(false);

    public void setReady() {
        ready.set(true);
    }

    public void setNotReady() {
        ready.set(false);
    }

    public boolean isReady() {
        return ready.get();
    }
}
