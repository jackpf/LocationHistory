package com.jackpf.locationhistory.client.ssl;

import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jackpf.locationhistory.client.R;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SSLPrompt {
    private final Activity activity;
    private final TrustedCertStorage storage;
    private final static AtomicBoolean isShowing = new AtomicBoolean(false);
    private static final long PROMPT_COOLDOWN_MS = 30_000;

    private final static Cache<String, Boolean> promptCache = CacheBuilder.newBuilder()
            .expireAfterWrite(PROMPT_COOLDOWN_MS, TimeUnit.MILLISECONDS)
            .build();

    public SSLPrompt(Activity activity) {
        this.activity = activity;
        this.storage = new TrustedCertStorage(activity);
    }

    private void closePrompt(String fingerprint, boolean accept) {
        promptCache.put(fingerprint, true);
        if (accept) {
            storage.addFingerprint(fingerprint);
        }
        isShowing.set(false);
    }

    public void show(String fingerprint, boolean force) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (!force && promptCache.getIfPresent(fingerprint) != null) {
            return;
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
                            .setOnDismissListener((dialog) -> {
                                closePrompt(fingerprint, false);
                            })
                            .show()
            );
        }
    }
}
