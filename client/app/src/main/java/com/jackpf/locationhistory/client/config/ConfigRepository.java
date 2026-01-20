package com.jackpf.locationhistory.client.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

public class ConfigRepository {
    private final Context context;
    private final SharedPreferences prefs;

    private static final String PREFERENCES_KEY = "beacon";

    public static final String DEVICE_ID_KEY = "device-id";
    public static final String DEVICE_READY_KEY = "device-ready";
    public static final String DEVICE_STATUS_KEY = "device-status";

    public static final String SERVER_HOST_KEY = "server-host";
    public static final String SERVER_PORT_KEY = "server-port";

    public static final String UPDATE_INTERVAL_KEY = "update-interval";

    public static final String LAST_RUN_TIMESTAMP_KEY = "last-run-timestamp";

    public ConfigRepository(Context context) {
        this.context = context;
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

    public String getDeviceName() {
        String deviceName = Settings.Global.getString(context.getContentResolver(), Settings.Global.DEVICE_NAME);
        return deviceName != null ? deviceName : "";
    }

    public boolean getDeviceReady() {
        return prefs.getBoolean(DEVICE_READY_KEY, false);
    }

    public void setDeviceReady(boolean deviceReady) {
        prefs.edit().putBoolean(DEVICE_READY_KEY, deviceReady).apply();
    }

    public void setDeviceStatus(String deviceStatus) {
        prefs.edit().putString(DEVICE_STATUS_KEY, deviceStatus).apply();
    }

    public String getDeviceStatus() {
        return prefs.getString(DEVICE_STATUS_KEY, "unknown");
    }

    public String getServerHost() {
        return prefs.getString(SERVER_HOST_KEY, "");
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

    public long getLastRunTimestamp() {
        return prefs.getLong(LAST_RUN_TIMESTAMP_KEY, 0L);
    }

    public long getUpdateIntervalMinutes() {
        return prefs.getLong(UPDATE_INTERVAL_KEY, 15L);
    }

    public void setUpdateIntervalMinutes(long minutes) {
        prefs.edit().putLong(UPDATE_INTERVAL_KEY, minutes).apply();
    }

    public void setLastRunTimestamp(long lastRunTime) {
        prefs.edit().putLong(LAST_RUN_TIMESTAMP_KEY, lastRunTime).apply();
    }
}
