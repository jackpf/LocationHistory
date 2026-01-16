package com.jackpf.locationhistory.client;

import android.Manifest;
import android.content.Context;
import android.os.Build;

import com.jackpf.locationhistory.client.permissions.AppRequirement;
import com.jackpf.locationhistory.client.permissions.IgnoreBatteryOptimizationsSetting;
import com.jackpf.locationhistory.client.permissions.Permission;

import java.util.ArrayList;
import java.util.List;

public class AppRequirements {
    public static List<AppRequirement> getRequirements(Context context) {
        List<AppRequirement> requirements = new ArrayList<>();

        requirements.add(new Permission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                context.getString(R.string.permission_desc_fine_location),
                context.getString(R.string.permission_exp_fine_location)
        ));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requirements.add(new Permission(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    context.getString(R.string.permission_desc_background_location),
                    context.getString(R.string.permission_exp_background_location)
            ));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requirements.add(new Permission(
                    Manifest.permission.POST_NOTIFICATIONS,
                    context.getString(R.string.permission_desc_post_notifications),
                    context.getString(R.string.permission_exp_post_notifications)
            ));
        }

        requirements.add(new IgnoreBatteryOptimizationsSetting(
                context.getString(R.string.permission_desc_ignore_optimisations),
                context.getString(R.string.permission_exp_ignore_optimisations)
        ));

        return requirements;
    }
}
