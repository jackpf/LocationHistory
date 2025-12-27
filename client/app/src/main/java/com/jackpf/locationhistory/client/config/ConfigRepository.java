package com.jackpf.locationhistory.client.config;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigRepository {
    private final SharedPreferences prefs;
    private static final String PREFERENCES_KEY = "beacon";
    private static final String DEVICE_ID_KEY = "device-id";
    private static final String PRIVATE_KEY_KEY = "private-key";
    private static final String PUBLIC_KEY_KEY = "private-key";
    private static final String SERVER_HOST_KEY = "server-host";
    private static final String SERVER_PORT_KEY = "server-port";
    private static final String UPDATE_INTERVAL_KEY = "update-interval";

    public ConfigRepository(Context context) {
        this.prefs = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public String getDeviceId() {
        return prefs.getString(DEVICE_ID_KEY, "");
    }

    public void setDeviceId(String deviceId) {
        prefs.edit().putString(DEVICE_ID_KEY, deviceId).apply();
    }

    public String getPrivateKey() {
        return prefs.getString(PRIVATE_KEY_KEY, "");
    }

    public void setPrivateKey(String privateKey) {
        prefs.edit().putString(PRIVATE_KEY_KEY, privateKey).apply();
    }

    public String getPublicKey() {
        return prefs.getString(PUBLIC_KEY_KEY, "");
    }

    public void setPublicKey(String publicKey) {
        prefs.edit().putString(PUBLIC_KEY_KEY, publicKey).apply();
    }

    public String getServerHost() {
        return prefs.getString(SERVER_HOST_KEY, "10.0.2.2");
    }

    public void setServerHost(String host) {
        prefs.edit().putString(SERVER_HOST_KEY, host).apply();
    }

    public int getServerPort() {
        return prefs.getInt(SERVER_PORT_KEY, 8080);
    }

    public void setServerPort(int port) {
        prefs.edit().putInt(SERVER_PORT_KEY, port).apply();
    }

    public long getUpdateIntervalMillis() {
        return prefs.getLong(UPDATE_INTERVAL_KEY, 10_000L);
    }

    public void setUpdateIntervalMillis(long updateInterval) {
        prefs.edit().putLong(UPDATE_INTERVAL_KEY, updateInterval).apply();
    }
}
