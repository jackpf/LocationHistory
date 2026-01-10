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
    private final static AtomicBoolean isShowing = new AtomicBoolean(false);
    private final Map<String, Long> promptCache = new HashMap<>();

    private final long PROMPT_COOLDOWN_MS = 30_000;

    public SSLPrompt(Activity activity) {
        this.activity = activity;
        this.storage = new TrustedCertStorage(activity);
    }

    private void closePrompt(String fingerprint, boolean accept) {
        promptCache.put(fingerprint, System.currentTimeMillis());
        if (accept) {
            storage.addFingerprint(fingerprint);
        }
        isShowing.set(false);
    }

    public void show(String fingerprint, boolean force) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (!force && promptCache.containsKey(fingerprint)) {
            long lastDenied = promptCache.get(fingerprint);
            if (System.currentTimeMillis() - lastDenied < PROMPT_COOLDOWN_MS) {
                return;
            }
        }

        if (isShowing.compareAndSet(false, true)) {
            activity.runOnUiThread(() ->
                    new AlertDialog.Builder(activity)
                            .setTitle(activity.getString(R.string.security_warning))
                            .setMessage(activity.getString(R.string.message_ssl_prompt, fingerprint))
                            .setPositiveButton(activity.getString(R.string.trust_always), (dialog, which) -> {
                                closePrompt(fingerprint, true);
                            })
                            .setNegativeButton(activity.getString(R.string.cancel), (dialog, which) -> {
                                closePrompt(fingerprint, false);
                            })
                            .setOnDismissListener((dialog) -> isShowing.set(false))
                            .show()
            );
        }
    }
}
