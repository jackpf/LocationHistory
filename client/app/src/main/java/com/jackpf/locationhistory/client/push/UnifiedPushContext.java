package com.jackpf.locationhistory.client.push;

import android.content.Context;

import androidx.annotation.NonNull;

import org.unifiedpush.android.connector.UnifiedPush;

import java.util.List;

public class UnifiedPushContext {
    private final Context context;

    public UnifiedPushContext(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public List<String> getDistributors() {
        return UnifiedPush.getDistributors(context);
    }

    public void register(String distributor) {
        UnifiedPushService.register(context, distributor);
    }

    public void unregister() {
        UnifiedPushService.unregister(context);
    }
}
