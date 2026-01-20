package com.jackpf.locationhistory.client.worker;

public class DeviceNotReadyException extends RetryableException {
    public DeviceNotReadyException() {
        super("Device not ready");
    }
}
