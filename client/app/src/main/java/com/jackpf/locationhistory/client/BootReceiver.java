package com.jackpf.locationhistory.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jackpf.locationhistory.client.util.Logger;

public class BootReceiver extends BroadcastReceiver {
    private final Logger log = new Logger(this);

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            log.i("Device rebooted. Restarting service...");

            BeaconService.startForegroundIfPermissionsGranted(
                    context,
                    AppRequirements.getRequirements(context)
            );
        }
    }
}
