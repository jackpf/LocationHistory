package com.jackpf.locationhistory.client.ssl;

import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

import com.jackpf.locationhistory.client.R;

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
                    .setTitle(activity.getString(R.string.security_warning))
                    .setMessage(activity.getString(R.string.message_ssl_prompt, fingerprint))
                    .setPositiveButton(activity.getString(R.string.trust_always), (dialog, which) -> {
                        isShowing.set(false);
                        storage.addFingerprint(fingerprint);
                    })
                    .setNegativeButton(activity.getString(R.string.cancel), (dialog, which) -> {
                        deniedCache.put(fingerprint, System.currentTimeMillis());
                        isShowing.set(false);
                    })
                    .setOnDismissListener((dialog) -> isShowing.set(false))
                    .show();
        }
    }
}
