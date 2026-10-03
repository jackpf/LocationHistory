package com.jackpf.locationhistory.client.push;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import org.unifiedpush.android.connector.UnifiedPush;

import java.util.List;

import static org.unifiedpush.android.connector.ConstantsKt.INSTANCE_DEFAULT;

/**
 * UnifiedPush implementation of {@link PushProvider}.
 * Registration/unregistration is initiated here; the actual server-side
 * handshake is handled by UnifiedPushService (in :app, foss source set) via
 * the onNewEndpoint / onUnregistered callbacks.
 */
public class UnifiedPushProvider implements PushProvider {

    /**
     * Must match the action declared in UnifiedPushService and the foss manifest.
     */
    private static final String CUSTOM_UNREGISTER_ACTION =
            "com.jackpf.locationhistory.client.MANUAL_UNREGISTER";

    /**
     * Fully-qualified class name of UnifiedPushService (in :app foss source set).
     * Using a string reference avoids a circular module dependency.
     */
    private static final String UNIFIED_PUSH_SERVICE_CLASS =
            "com.jackpf.locationhistory.client.push.UnifiedPushService";

    private final Context context;

    public UnifiedPushProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public List<String> getDistributors() {
        return UnifiedPush.getDistributors(context);
    }

    @Override
    public boolean requiresDistributorApp() {
        return true;
    }

    @Override
    public void register(@Nullable String distributor) {
        if (distributor != null) {
            UnifiedPush.saveDistributor(context, distributor);
        }
        UnifiedPush.register(context, INSTANCE_DEFAULT, "", null);
    }

    @Override
    public void unregister() {
        UnifiedPush.unregister(context, INSTANCE_DEFAULT);

        // UnifiedPush does not call onUnregistered automatically, so we fire a
        // custom intent to UnifiedPushService to trigger server-side cleanup.
        Intent intent = new Intent();
        intent.setClassName(context.getPackageName(), UNIFIED_PUSH_SERVICE_CLASS);
        intent.setAction(CUSTOM_UNREGISTER_ACTION);
        intent.putExtra("instance", INSTANCE_DEFAULT);
        context.startService(intent);
    }

    @Override
    public void promptDistributorInstall(Context context) {
        Ntfy.promptInstall(context);
    }
}
