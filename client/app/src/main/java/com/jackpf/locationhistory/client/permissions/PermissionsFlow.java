package com.jackpf.locationhistory.client.permissions;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.jackpf.locationhistory.client.util.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class PermissionsFlow {

    private final Activity activity;
    private final List<String[]> permissionsSteps = new ArrayList<>();
    private Optional<Runnable> onCompleteAction;

    private final Logger log = new Logger(this);

    public PermissionsFlow(Activity activity) {
        this.activity = activity;
    }

    public PermissionsFlow require(String[] permissions) {
        log.d("Requiring permissions %s", Arrays.toString(permissions));
        permissionsSteps.add(permissions);
        return this;
    }

    public PermissionsFlow thenRequire(String[] permissions) {
        return require(permissions);
    }

    public PermissionsFlow onComplete(Runnable action) {
        log.d("Setting onComplete action");
        onCompleteAction = Optional.of(action);
        return this;
    }

    private void requestPermissions(int step) {
        String[] permissions = permissionsSteps.get(step);
        log.d("Requesting permissions %s", Arrays.toString(permissions));
        ActivityCompat.requestPermissions(activity, permissions, step);
    }

    public void start() {
        log.d("Starting permissions flow");
        if (!permissionsSteps.isEmpty()) {
            requestPermissions(0);
        } else {
            throw new IllegalStateException("No permissions to request");
        }
    }

    public void onRequestPermissionsResult(int code, @NonNull String[] permissions, @NonNull int[] grantResults, Function<String, Boolean> onDenied) {
        log.d("Handling permissions request %d", code);

        boolean shouldContinue = true;

        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                String permissionName = permissions[i];
                log.i("Permission %s was denied", permissionName);
                if (!onDenied.apply(permissionName)) {
                    shouldContinue = false;
                }
            }
        }

        if (shouldContinue) {
            if (permissionsSteps.size() > code + 1) {
                requestPermissions(code + 1);
            } else onCompleteAction.ifPresent(Runnable::run);
        } else {
            log.w("Aborting permissions flow");
        }
    }
}
