package com.jackpf.locationhistory.client.model;

import com.jackpf.locationhistory.DeviceStatus;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.util.Logger;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DeviceState {
    private static final Logger log = new Logger("DeviceState");

    @Getter(AccessLevel.NONE)
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private String deviceId;
    private String deviceName;
    private DeviceStatus deviceStatus;

    // Last run values
    private long lastRunTimestamp;

    private static String generateDeviceId() {
        return UUID.randomUUID().toString();
    }

    public DeviceState setReady() {
        ready.set(true);
        return this;
    }

    public DeviceState setNotReady() {
        ready.set(false);
        return this;
    }

    public DeviceState setIsReady(boolean isReady) {
        ready.set(isReady);
        return this;
    }

    public boolean isReady() {
        return ready.get();
    }

    public DeviceState updateFromStatus(DeviceStatus status) {
        if (status == DeviceStatus.DEVICE_REGISTERED) setReady();
        else setNotReady();
        deviceStatus = status;
        return this;
    }

    public void storeToConfig(ConfigRepository configRepository) {
        configRepository.setDeviceId(deviceId);
        configRepository.setDeviceReady(ready.get());
        configRepository.setDeviceStatus(deviceStatus.toString());
        configRepository.setLastRunTimestamp(lastRunTimestamp);
    }

    public static DeviceState fromConfig(ConfigRepository configRepository) {
        String deviceId = configRepository.getDeviceId();
        if ("".equals(deviceId)) deviceId = generateDeviceId();

        DeviceStatus thisDeviceStatus = DeviceStatus.DEVICE_UNKNOWN;
        try {
            thisDeviceStatus = DeviceStatus.valueOf(configRepository.getDeviceStatus());
        } catch (IllegalArgumentException e) {
            log.e(e, "Invalid stored device status: %s", configRepository.getDeviceStatus());
        }

        return new DeviceState()
                .setDeviceId(deviceId)
                .setDeviceName(configRepository.getDeviceName())
                .setIsReady(configRepository.getDeviceReady())
                .setDeviceStatus(thisDeviceStatus)
                .setLastRunTimestamp(configRepository.getLastRunTimestamp());
    }
}
