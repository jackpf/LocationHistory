package com.jackpf.locationhistory.client.model;

import com.jackpf.locationhistory.DeviceStatus;
import com.jackpf.locationhistory.client.config.ConfigRepository;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DeviceState {
    @Getter(AccessLevel.NONE)
    private final AtomicBoolean ready = new AtomicBoolean(false);
    String deviceId;
    String deviceName;
    String publicKey;

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

    public void updateFromStatus(DeviceStatus status) {
        if (status == DeviceStatus.DEVICE_REGISTERED) setReady();
        else setNotReady();
    }

    public void storeToConfig(ConfigRepository configRepository) {
        configRepository.setDeviceId(deviceId);
        configRepository.setPublicKey(publicKey);
        configRepository.setDeviceReady(ready.get());
    }

    public static DeviceState fromConfig(ConfigRepository configRepository) {
        String deviceId = configRepository.getDeviceId();
        if ("".equals(deviceId)) deviceId = generateDeviceId();

        return new DeviceState()
                .setDeviceId(deviceId)
                .setDeviceName(configRepository.getDeviceName())
                .setIsReady(configRepository.getDeviceReady())
                .setPublicKey(configRepository.getPublicKey());
    }
}
