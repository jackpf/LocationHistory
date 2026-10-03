package com.jackpf.locationhistory.client.push;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Generic SharedPreferences storage for push provider state (enabled flag, endpoint).
 * Renamed from UnifiedPushStorage to be backend-agnostic.
 */
public class PushStorage {
    private static final String PREFERENCES_KEY = "PushProvider";
    private static final String ENDPOINT_KEY = "endpoint";
    private static final String ENABLED_KEY = "enabled";

    private final Context context;
    private final SharedPreferences prefs;

    public PushStorage(Context context) {
        this.context = context;
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
        // Notify LiveData observers
        ObservablePushState.getInstance(context).setEnabled(enabled);
    }
}
