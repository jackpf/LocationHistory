package com.jackpf.locationhistory.client.push;

import android.content.Context;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Abstraction over push notification backends (UnifiedPush, FCM, etc.).
 * Implementations live in flavour-specific modules (:push-unifiedpush, :push-firebase).
 */
public interface PushProvider {

    /**
     * Returns available distributor/backend options for selection in the UI.
     * Returns an empty list for providers that auto-select (e.g. FCM).
     */
    List<String> getDistributors();

    /**
     * Returns true if an external app (e.g. a UnifiedPush distributor) must be
     * installed before registration can succeed. When true and
     * {@link #getDistributors()} is empty, the UI should prompt the user to
     * install the distributor app via {@link #promptDistributorInstall(Context)}.
     * Returns false for providers that do not need an external app (e.g. FCM).
     */
    boolean requiresDistributorApp();

    /**
     * Initiates registration with the given distributor/backend identifier.
     * Pass {@code null} for providers that do not require a distributor (e.g. FCM).
     */
    void register(@Nullable String distributor);

    /**
     * Unregisters from the push backend and notifies the server.
     */
    void unregister();

    /**
     * Prompts the user to install the required distributor app.
     * No-op for providers that do not need an external app.
     */
    void promptDistributorInstall(Context context);
}
