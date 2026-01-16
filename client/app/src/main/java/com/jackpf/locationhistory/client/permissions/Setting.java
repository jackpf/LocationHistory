package com.jackpf.locationhistory.client.permissions;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import lombok.Getter;

public abstract class Setting implements AppRequirement {
    @Getter
    final String name;
    @Getter
    final String description;
    @Getter
    final String explanation;

    private ActivityResultLauncher<Intent> launcher;

    public Setting(String name, String description, String explanation) {
        this.name = name;
        this.description = description;
        this.explanation = explanation;
    }

    @Override
    public AppRequirement register(ActivityResultCaller caller, Runnable onResult) {
        launcher = caller.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> onResult.run()
        );
        return this;
    }

    @Override
    public void request(Context context) {
        if (launcher == null)
            throw new IllegalStateException("Unable to request permission - no launcher registered");

        Intent intent = new Intent();
        intent.setAction(name);
        intent.setData(Uri.parse("package:" + context.getPackageName()));

        launcher.launch(intent);
    }
}
