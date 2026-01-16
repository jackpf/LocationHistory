package com.jackpf.locationhistory.client.permissions;

import android.content.Context;

import java.util.List;

public class AppRequirementsUtil {
    AppRequirementsUtil() {
    }

    public static boolean allGranted(Context context, List<AppRequirement> requirements) {
        for (AppRequirement requirement : requirements) {
            if (!requirement.isGranted(context)) {
                return false;
            }
        }
        return true;
    }
}
