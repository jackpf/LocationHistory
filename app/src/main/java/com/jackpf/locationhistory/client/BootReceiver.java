package com.jackpf.locationhistory.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        // TODO Check permissions
        // TODO Create & check "start on boot" option
//        c.startForegroundService(new Intent(c, BeaconService.class));
    }
}
