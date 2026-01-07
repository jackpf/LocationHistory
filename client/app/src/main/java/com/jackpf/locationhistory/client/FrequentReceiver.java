package com.jackpf.locationhistory.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class FrequentReceiver extends BroadcastReceiver {
    public static String PERIOD_MILLIS = "period";

    @Override
    public void onReceive(Context context, Intent intent) {
        BeaconWorkerFactory.scheduleOnce(context);

        int periodMillis = intent.getIntExtra(PERIOD_MILLIS, -1);

        if (periodMillis > 0) {
            BeaconWorkerFactory.scheduleFrequent(context, periodMillis);
        }
    }
}
