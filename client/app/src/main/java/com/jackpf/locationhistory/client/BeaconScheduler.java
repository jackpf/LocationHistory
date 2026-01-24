package com.jackpf.locationhistory.client;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;

import com.google.common.util.concurrent.AsyncCallable;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.jackpf.locationhistory.client.util.Logger;

import java.util.concurrent.TimeUnit;

public class BeaconScheduler {
    private final Logger log = new Logger(this);
    private final AlarmManager alarmManager;
    private final PowerManager.WakeLock wakeLock;
    private final long wakelockTimeout;

    private static final String WAKELOCK_TAG = "BeaconScheduler:wakelock";
    public static final long DEFAULT_WAKELOCK_TIMEOUT = TimeUnit.MINUTES.toMillis(1);

    public BeaconScheduler(AlarmManager alarmManager,
                           PowerManager.WakeLock wakeLock,
                           long wakelockTimeout) {
        this.alarmManager = alarmManager;
        this.wakeLock = wakeLock;
        this.wakelockTimeout = wakelockTimeout;
    }

    public static BeaconScheduler create(Context context, long wakelockTimeout) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
        wakeLock.setReferenceCounted(false); // Ensure we don't double-acquire

        return new BeaconScheduler(
                alarmManager,
                wakeLock,
                wakelockTimeout
        );
    }

    public <T> ListenableFuture<T> runWithWakeLock(AsyncCallable<T> taskFactory) {
        acquireLock();

        try {
            ListenableFuture<T> taskFuture = taskFactory.call();
            taskFuture.addListener(this::releaseLock, MoreExecutors.directExecutor());
            return taskFuture;
        } catch (Exception e) {
            releaseLock();
            return Futures.immediateFailedFuture(e);
        }
    }

    private PendingIntent createPendingIntent(Context context, Class<?> cls, String action) {
        Intent intent = new Intent(context, cls);
        intent.setAction(action);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PendingIntent.getForegroundService(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } else {
            return PendingIntent.getService(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );
        }
    }

    private void schedulePendingIntent(PendingIntent pendingIntent, long delayMillis) {
        long triggerAtMillis = SystemClock.elapsedRealtime() + delayMillis;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public void scheduleNext(Context context, Class<?> cls, String action, long delayMillis) {
        log.d("Scheduling next task for now + %dms", delayMillis);

        PendingIntent pendingIntent = createPendingIntent(context, cls, action);
        schedulePendingIntent(pendingIntent, delayMillis);
    }

    private void acquireLock() {
        if (!wakeLock.isHeld()) {
            wakeLock.acquire(wakelockTimeout);
        }
    }

    private void releaseLock() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}
