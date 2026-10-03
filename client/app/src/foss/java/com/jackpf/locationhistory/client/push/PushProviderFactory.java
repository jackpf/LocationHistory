package com.jackpf.locationhistory.client.push;

import android.app.Application;

/**
 * FOSS flavour factory — returns a {@link UnifiedPushProvider}.
 * The corresponding GMS factory lives in app/src/gms/.
 */
public class PushProviderFactory {
    public static PushProvider create(Application application) {
        return new UnifiedPushProvider(application);
    }
}
