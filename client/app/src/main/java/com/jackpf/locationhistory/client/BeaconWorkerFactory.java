package com.jackpf.locationhistory.client;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BeaconWorkerFactory {
    public static void createWorker(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest beaconRequest =
                new PeriodicWorkRequest.Builder(BeaconWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "BeaconLocationSync",
                ExistingPeriodicWorkPolicy.KEEP,
                beaconRequest
        );
    }

    public static void createTestWorker(Context context, int period, TimeUnit timeUnit) {
        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
            scheduler.scheduleAtFixedRate(() -> {
                WorkManager.getInstance(context).enqueueUniqueWork(
                        "BeaconLocationSync_Test",
                        ExistingWorkPolicy.REPLACE,
                        new OneTimeWorkRequest.Builder(BeaconWorker.class).build()
                );
            }, 0, period, timeUnit);
        }
    }
}
