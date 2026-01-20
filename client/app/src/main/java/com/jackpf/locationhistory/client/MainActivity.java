package com.jackpf.locationhistory.client;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.DynamicColors;
import com.jackpf.locationhistory.client.permissions.AppRequirementsUtil;
import com.jackpf.locationhistory.client.util.Logger;

public class MainActivity extends AppCompatActivity {
    private final Logger log = new Logger(this);

    @Override
    protected void onCreate(Bundle bundle) {
        DynamicColors.applyToActivitiesIfAvailable(getApplication());
        super.onCreate(bundle);

        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (AppRequirementsUtil.allGranted(this, AppRequirements.getRequirements(this))) {
            log.d("Permissions granted, starting service");
            ContextCompat.startForegroundService(this, new Intent(this, BeaconService.class));
        } else {
            log.w("Permissions not yet granted, not starting service");
        }
    }
}
