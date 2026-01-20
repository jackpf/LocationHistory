package com.jackpf.locationhistory.client;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

        ContextCompat.startForegroundService(this, new Intent(this, BeaconService.class));
    }
}
