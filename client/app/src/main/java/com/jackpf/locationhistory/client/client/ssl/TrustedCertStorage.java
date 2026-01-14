package com.jackpf.locationhistory.client.client.ssl;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class TrustedCertStorage {
    private static final String PREFERENCES_KEY = "TrustedCerts";
    private static final String FINGERPRINTS_KEY = "fingerprints";
    private final SharedPreferences prefs;

    public TrustedCertStorage(Context context) {
        this.prefs = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public Set<String> getTrustedFingerprints() {
        return prefs.getStringSet(FINGERPRINTS_KEY, new HashSet<>());
    }

    public void addFingerprint(String fingerprint) {
        Set<String> current = new HashSet<>(getTrustedFingerprints());
        current.add(fingerprint);

        prefs.edit().putStringSet(FINGERPRINTS_KEY, current).apply();
    }
}
