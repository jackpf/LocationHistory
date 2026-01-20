package com.jackpf.locationhistory.client;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.permissions.AppRequirement;
import com.jackpf.locationhistory.client.permissions.AppRequirementsUtil;
import com.jackpf.locationhistory.client.ui.Notifications;
import com.jackpf.locationhistory.client.util.Logger;
import com.jackpf.locationhistory.client.worker.BeaconResult;
import com.jackpf.locationhistory.client.worker.BeaconTask;
import com.jackpf.locationhistory.client.worker.RetryableException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BeaconService extends Service {
    private static final Logger log = new Logger("BeaconService");
    private ExecutorService executorService;
    private ConfigRepository configRepository;
    private BeaconScheduler beaconScheduler;
    private static final int PERSISTENT_NOTIFICATION_ID = 1;
    private static final String ACTION_RUN_TASK = "com.jackpf.locationhistory.client.ACTION_BEACON_SERVICE";
    private static final long WAKELOCK_TIMEOUT = TimeUnit.MINUTES.toMillis(10);

    private static final String START_MESSAGE = "Beacon task started";
    private static final String SUCCESS_MESSAGE = "Beacon task completed successfully";
    private static final String FAILED_MESSAGE = "Beacon task failed";
    private static final String RETRY_MESSAGE = "Beacon task failed with retry";

    private final Runnable beaconTask = () ->
            beaconScheduler.runWithWakeLock(() -> {
                log.i(START_MESSAGE);
                log.appendEventToFile(BeaconService.this, START_MESSAGE);

                ListenableFuture<BeaconResult> beaconResult = BeaconTask
                        .runSafe(BeaconService.this, executorService);

                Futures.addCallback(beaconResult, new FutureCallback<BeaconResult>() {
                    @Override
                    public void onSuccess(BeaconResult result) {
                        log.i("%s: %s", SUCCESS_MESSAGE, result);
                        log.appendEventToFile(BeaconService.this, "%s: %s", SUCCESS_MESSAGE, result);
                        scheduleNext(regularDelayMillis());
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        if (t instanceof RetryableException) {
                            log.w(RETRY_MESSAGE, t);
                            log.appendEventToFile(BeaconService.this, "%s: %s", RETRY_MESSAGE, t.getMessage());
                            scheduleNext(retryDelayMillis());
                        } else {
                            log.e(FAILED_MESSAGE, t);
                            log.appendEventToFile(BeaconService.this, "%s: %s", FAILED_MESSAGE, t.getMessage());
                            scheduleNext(regularDelayMillis());
                        }
                    }
                }, ContextCompat.getMainExecutor(BeaconService.this));

                return beaconResult;
            });

    private void scheduleNext(long delayMillis) {
        beaconScheduler.scheduleNext(this, BeaconService.class, ACTION_RUN_TASK, delayMillis);
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener configChangeListener = (prefs, key) -> {
        if (key != null &&
                (key.equals(ConfigRepository.SERVER_HOST_KEY)
                        || key.equals(ConfigRepository.SERVER_PORT_KEY)
                        || key.equals(ConfigRepository.UPDATE_INTERVAL_KEY)
                        || key.equals(ConfigRepository.HIGH_ACCURACY_TRIGGERED_AT_KEY)
                )) {
            log.d("Detected config change, caused by %s", key);
            scheduleNext(500); // Debounce config changes with 500ms delay
        }
    };

    private long regularDelayMillis() {
        return TimeUnit.MINUTES.toMillis(configRepository.getUpdateIntervalMinutes());
    }

    private long highAccuracyDelayMillis() {
        // Trigger frequently in high accuracy mode
        return TimeUnit.SECONDS.toMillis(60);
    }

    private long retryDelayMillis() {
        return TimeUnit.SECONDS.toMillis(30);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        executorService = Executors.newSingleThreadExecutor();

        configRepository = new ConfigRepository(this);
        configRepository.registerOnSharedPreferenceChangeListener(configChangeListener);

        beaconScheduler = BeaconScheduler.create(this, WAKELOCK_TIMEOUT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Notifications notifications = new Notifications(this);
        ServiceCompat.startForeground(this,
                PERSISTENT_NOTIFICATION_ID,
                notifications.createPersistentNotification(
                        getString(R.string.persistent_notification_title),
                        configRepository.inHighAccuracyMode() ? getString(R.string.persistent_notification_high_accuracy_mode)
                                : getString(R.string.persistent_notification_message)
                ),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION : 0);

        if (intent == null || ACTION_RUN_TASK.equals(intent.getAction())) {
            beaconTask.run();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (executorService != null) executorService.shutdown();
        if (configRepository != null)
            configRepository.unregisterOnSharedPreferenceChangeListener(configChangeListener);
    }

    public static void startForeground(Context context) {
        log.d("Starting service");
        Intent intent = new Intent(context, BeaconService.class);
        intent.setAction(ACTION_RUN_TASK);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void startForegroundIfPermissionsGranted(Context context,
                                                           List<AppRequirement> requirements) {
        if (AppRequirementsUtil.allGranted(context, requirements)) {
            log.d("Permissions granted");
            startForeground(context);
        } else {
            log.w("Permissions not yet granted");
        }
    }
}
