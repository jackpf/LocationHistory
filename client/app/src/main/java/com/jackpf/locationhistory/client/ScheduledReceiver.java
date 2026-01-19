package com.jackpf.locationhistory.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jackpf.locationhistory.client.util.Logger;
import com.jackpf.locationhistory.client.util.PermissionException;
import com.jackpf.locationhistory.client.worker.BeaconWorkerFactory;

public class ScheduledReceiver extends BroadcastReceiver {
    private final Logger log = new Logger(this);

    public static final String PERIOD_MILLIS = "period";

    @Override
    public void onReceive(Context context, Intent intent) {
        BeaconWorkerFactory.runOnce(context);

        long periodMillis = intent.getLongExtra(PERIOD_MILLIS, -1L);

        if (periodMillis > 0L) {
            try {
                BeaconWorkerFactory.scheduleFrequent(context, periodMillis);
            } catch (PermissionException e) {
                log.e("Unable to re-schedule alarm", e);
            }
        }
    }
}
