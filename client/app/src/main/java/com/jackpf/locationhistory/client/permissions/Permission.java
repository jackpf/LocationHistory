package com.jackpf.locationhistory.client.permissions;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import lombok.Getter;

public class Permission implements AppRequirement {
    @Getter
    final String name;
    @Getter
    final String description;
    @Getter
    final String explanation;

    private ActivityResultLauncher<String> launcher;

    public Permission(String name, String description, String explanation) {
        this.name = name;
        this.description = description;
        this.explanation = explanation;
    }

    @Override
    public AppRequirement register(ActivityResultCaller caller, Runnable onResult) {
        launcher = caller.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> onResult.run()
        );

        return this;
    }

    @Override
    public boolean isGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, name)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void request(Context context) {
        if (launcher == null)
            throw new IllegalStateException("Unable to request permission - no launcher registered");
        launcher.launch(name);
    }
}
