package com.jackpf.locationhistory.client.push;

import android.content.Context;
import android.content.SharedPreferences;

public class UnifiedPushStorage {
    private static final String PREFERENCES_KEY = "UnifiedPush";
    private static final String ENDPOINT_KEY = "endpoint";
    private static final String ENABLED_KEY = "enabled";
    private final SharedPreferences prefs;

    public UnifiedPushStorage(Context context) {
        this.prefs = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public String getEndpoint() {
        return prefs.getString(ENDPOINT_KEY, "");
    }

    public void setEndpoint(String endpoint) {
        prefs.edit().putString(ENDPOINT_KEY, endpoint).apply();
    }

    public boolean isEnabled() {
        return prefs.getBoolean(ENABLED_KEY, false);
    }

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(ENABLED_KEY, enabled).apply();
    }
}
