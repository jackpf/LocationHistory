package com.jackpf.locationhistory.client;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle bundle) {
        DynamicColors.applyToActivitiesIfAvailable(getApplication());
        super.onCreate(bundle);

        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start our worker!
        // Multiple calls are fine since we use ExistingPeriodicWorkPolicy.KEEP
        BeaconWorkerFactory.createWorker(this);
    }

}
