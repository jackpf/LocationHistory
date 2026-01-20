package com.jackpf.locationhistory.client;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;
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

        BeaconService.startForegroundIfPermissionsGranted(this);
    }
}
