package com.jackpf.locationhistory.client.permissions;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

public class IgnoreBatteryOptimizationsSetting extends Setting {
    public IgnoreBatteryOptimizationsSetting(String description, String explanation) {
        super(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, description, explanation);
    }

    @Override
    public boolean isGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(android.content.Context.POWER_SERVICE);
            return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true;
    }
}
