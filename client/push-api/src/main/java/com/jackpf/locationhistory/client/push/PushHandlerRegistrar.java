package com.jackpf.locationhistory.client.push;

/**
 * Abstracts server-side push handler registration so that push modules
 * (:push-firebase) can trigger registration without depending on :app classes
 * (ConfigRepository, BeaconClient, etc.).
 *
 * Implementations live in :app and are injected via PushProviderFactory.
 */
public interface PushHandlerRegistrar {

    /**
     * Registers a push endpoint with the server.
     *
     * @param name     human-readable backend name, e.g. "FCM" or "UnifiedPush"
     * @param endpoint push endpoint URL or FCM token
     */
    void registerPushHandler(String name, String endpoint);

    /**
     * Unregisters the push handler from the server.
     */
    void unregisterPushHandler();
}
