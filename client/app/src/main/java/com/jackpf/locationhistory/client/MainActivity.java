package com.jackpf.locationhistory.client;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.ui.Toasts;
import com.jackpf.locationhistory.client.util.Logger;
import com.jackpf.locationhistory.client.util.PermissionException;
import com.jackpf.locationhistory.client.worker.BeaconWorkerFactory;

public class MainActivity extends AppCompatActivity {
    private final Logger log = new Logger(this);

    private ConfigRepository configRepository;

    @Override
    protected void onCreate(Bundle bundle) {
        DynamicColors.applyToActivitiesIfAvailable(getApplication());
        super.onCreate(bundle);

        setContentView(R.layout.activity_main);

        configRepository = new ConfigRepository(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            BeaconWorkerFactory.schedule(this, configRepository);
        } catch (PermissionException e) {
            log.e("Unable to schedule beacon worker", e);
            Toasts.show(this, R.string.schedule_error);
        }
    }

}
