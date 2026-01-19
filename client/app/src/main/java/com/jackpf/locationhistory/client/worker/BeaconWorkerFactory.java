package com.jackpf.locationhistory.client.worker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.jackpf.locationhistory.client.BeaconService;
import com.jackpf.locationhistory.client.ScheduledReceiver;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.util.Logger;
import com.jackpf.locationhistory.client.util.PermissionException;

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

    public static void runOnce(Context context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
                "BeaconLocationSync_On_Demand",
                ExistingWorkPolicy.REPLACE,
                new OneTimeWorkRequest.Builder(BeaconWorker.class).build()
        );
    }

    private static PendingIntent frequentPendingIntent(Context context, long periodMillis) {
        Intent intent = new Intent(context, ScheduledReceiver.class);
        intent.putExtra(ScheduledReceiver.PERIOD_MILLIS, periodMillis);

        return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public static void scheduleFrequent(Context context, long periodMillis) throws PermissionException {
        log.d("Scheduling frequent beacon worker every %dms", periodMillis);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            throw new PermissionException("Unable to schedule exact alarm");
        }

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

    public static void schedule(Context context, ConfigRepository configRepository) throws PermissionException {
        // Cancel any existing workers
        BeaconWorkerFactory.cancelFrequent(context);
//        BeaconWorkerFactory.cancelPeriodic(context);
        Intent beaconServiceIntent = new Intent(context, BeaconService.class);
        context.stopService(beaconServiceIntent);

        // Start new worker!
        switch (configRepository.getUpdateFrequency()) {
            case ConfigRepository.UPDATE_FREQUENCY_BALANCED:
                BeaconWorkerFactory.schedulePeriodic(context);
                break;
            case ConfigRepository.UPDATE_FREQUENCY_HIGH:
//                long updateMillis = (long) configRepository.getUpdateIntervalMinutes() * 60 * 1000;
//                BeaconWorkerFactory.scheduleFrequent(context, updateMillis);
                ContextCompat.startForegroundService(context, new Intent(context, BeaconService.class));
                break;
        }
    }
}
