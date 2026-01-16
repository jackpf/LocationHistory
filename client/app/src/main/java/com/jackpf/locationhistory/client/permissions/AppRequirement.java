package com.jackpf.locationhistory.client.permissions;

import android.content.Context;

import androidx.activity.result.ActivityResultCaller;

public interface AppRequirement {
    String getName();

    String getDescription();

    String getExplanation();

    AppRequirement register(ActivityResultCaller caller, Runnable onResult);

    boolean isGranted(Context context);

    void request(Context context);
}
