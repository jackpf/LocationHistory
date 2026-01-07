package com.jackpf.locationhistory.client.ssl;

import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SSLPrompt {
    private final Activity activity;
    private final TrustedCertStorage storage;
    private final AtomicBoolean isShowing = new AtomicBoolean(false);
    private final Map<String, Long> deniedCache = new HashMap<>();

    private final long PROMPT_COOLDOWN_MS = 30_000;

    public SSLPrompt(Activity activity) {
        this.activity = activity;
        this.storage = new TrustedCertStorage(activity);
    }

    public void show(String fingerprint) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (deniedCache.containsKey(fingerprint)) {
            long lastDenied = deniedCache.get(fingerprint);
            if (System.currentTimeMillis() - lastDenied < PROMPT_COOLDOWN_MS) {
                return;
            }
        }

        if (isShowing.compareAndSet(false, true)) {
            new AlertDialog.Builder(activity)
                    // TODO Make strings
                    .setTitle("Security Warning")
                    .setMessage("Please verify this fingerprint against your server's logs.\nOnly click trust if they match.\n\nFingerprint:\n" + fingerprint)
                    .setPositiveButton("Trust Always", (dialog, which) -> {
                        isShowing.set(false);
                        storage.addFingerprint(fingerprint);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        deniedCache.put(fingerprint, System.currentTimeMillis());
                        isShowing.set(false);
                    })
                    .setOnDismissListener((dialog) -> isShowing.set(false))
                    .show();
        }
    }
}
