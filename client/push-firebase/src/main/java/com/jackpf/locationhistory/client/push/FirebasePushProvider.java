package com.jackpf.locationhistory.client.push;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Collections;
import java.util.List;

/**
 * Firebase Cloud Messaging implementation of {@link PushProvider}.
 *
 * Registration flow:
 *   1. register(null) is called by the ViewModel.
 *   2. FirebaseMessaging fetches (or returns a cached) FCM token.
 *   3. The token is registered with the server via {@link PushHandlerRegistrar}.
 *
 * Additionally, FirebasePushService.onNewToken() in :app (gms source set) handles
 * automatic token rotation by re-registering whenever Firebase issues a new token.
 */
public class FirebasePushProvider implements PushProvider {

    private static final String BACKEND_NAME = "FCM";

    private final Context context;
    @Nullable
    private final PushHandlerRegistrar registrar;
    private final PushStorage storage;

    public FirebasePushProvider(Context context,
                                @Nullable PushHandlerRegistrar registrar,
                                PushStorage storage) {
        this.context = context.getApplicationContext();
        this.registrar = registrar;
        this.storage = storage;
    }

    @Override
    public List<String> getDistributors() {
        // FCM has no user-selectable distributors; registration is automatic.
        return Collections.emptyList();
    }

    @Override
    public boolean requiresDistributorApp() {
        // FCM is self-contained; no external distributor app is needed.
        return false;
    }

    @Override
    public void register(@Nullable String distributor) {
        if (registrar == null) {
            return;
        }
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> registrar.registerPushHandler(BACKEND_NAME, token))
                .addOnFailureListener(e -> storage.setEnabled(false));
    }

    @Override
    public void unregister() {
        if (registrar != null) {
            registrar.unregisterPushHandler();
        } else {
            storage.setEnabled(false);
        }
    }

    @Override
    public void promptDistributorInstall(Context context) {
        // No-op: FCM does not require an external distributor app.
    }
}
