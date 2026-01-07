package com.jackpf.locationhistory.client;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.jackpf.locationhistory.client.util.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BeaconWorkerFactory {
    private static final Logger log = new Logger("BeaconWorkerFactory");

    private static final String WORK_NAME = "BeaconLocationSync";

    public static void schedulePeriodic(Context context) {
        log.d("Scheduling periodic beacon worker");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest beaconRequest =
                new PeriodicWorkRequest.Builder(BeaconWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                beaconRequest
        );
    }

    public static void cancelPeriodic(Context context) {
        log.d("Cancelling periodic beacon worker");

        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }

    public static void scheduleOnce(Context context) {
        log.d("Scheduling once beacon worker");

        WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME + "_On_Demand",
                ExistingWorkPolicy.REPLACE,
                new OneTimeWorkRequest.Builder(BeaconWorker.class)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
        );
    }

    private static PendingIntent frequentPendingIntent(Context context, long periodMillis) {
        Intent intent = new Intent(context, FrequentReceiver.class);
        intent.putExtra(FrequentReceiver.PERIOD_MILLIS, periodMillis);

        return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public static void scheduleFrequent(Context context, int periodMillis) {
        log.d("Scheduling frequent beacon worker");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = frequentPendingIntent(context, periodMillis);

        long triggerTime = System.currentTimeMillis() + periodMillis;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    public static void cancelFrequent(Context context) {
        log.d("Cancelling frequent beacon worker");

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = frequentPendingIntent(context, -1);
        am.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    public static void scheduleTestWorker(Context context, int period, TimeUnit timeUnit) {
        log.d("Scheduling test beacon worker");

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME + "_Test",
                    ExistingWorkPolicy.REPLACE,
                    new OneTimeWorkRequest.Builder(BeaconWorker.class).build()
            );
        }, 0, period, timeUnit);
    }
}
