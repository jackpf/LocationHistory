package com.jackpf.locationhistory.client;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationManager;
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
import com.jackpf.locationhistory.client.location.LocationData;
import com.jackpf.locationhistory.client.location.PassiveLocationListener;
import com.jackpf.locationhistory.client.permissions.AppRequirement;
import com.jackpf.locationhistory.client.permissions.AppRequirementsUtil;
import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.ui.Notifications;
import com.jackpf.locationhistory.client.util.Logger;
import com.jackpf.locationhistory.client.worker.BeaconResult;
import com.jackpf.locationhistory.client.worker.BeaconTask;
import com.jackpf.locationhistory.client.worker.RetryableException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BeaconService extends Service {
    private static final Logger log = new Logger("BeaconService");
    private ExecutorService executorService;
    private ConfigRepository configRepository;
    private BeaconScheduler beaconScheduler;
    private BeaconTask beaconTask;
    private PassiveLocationListener passiveLocationListener;
    private static final long PASSIVE_LISTENER_MIN_TIME_MS = TimeUnit.MINUTES.toMillis(1);
    private static final float PASSIVE_LISTENER_MIN_DISTANCE_M = 0L;
    private static final int PERSISTENT_NOTIFICATION_ID = 1;
    private static final String ACTION_RUN_TASK = "com.jackpf.locationhistory.client.ACTION_BEACON_SERVICE";
    private static final String ACTION_PASSIVE_LOCATION = "com.jackpf.locationhistory.client.ACTION_PASSIVE_LOCATION";

    private final Runnable beaconTaskRunnable = () ->
            beaconScheduler.runWithWakeLock(() -> {
                ListenableFuture<BeaconResult> beaconResult = beaconTask.run();

                Futures.addCallback(beaconResult, new FutureCallback<BeaconResult>() {
                    @Override
                    public void onSuccess(BeaconResult result) {
                        scheduleNext(regularDelayMillis());
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        if (t instanceof RetryableException) {
                            scheduleNext(retryDelayMillis());
                        } else {
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

        beaconScheduler = BeaconScheduler.create(this, BeaconScheduler.DEFAULT_WAKELOCK_TIMEOUT);
        passiveLocationListener = new PassiveLocationListener(this, new PermissionsManager(this),
                ACTION_PASSIVE_LOCATION, BeaconService.class);

        // Listen for passive location updates
        try {
            passiveLocationListener.startMonitoring(PASSIVE_LISTENER_MIN_TIME_MS, PASSIVE_LISTENER_MIN_DISTANCE_M);
        } catch (Exception e) {
            log.e("Unable to start passive listener");
        }
    }

    private void handleRunAction() {
        beaconTaskRunnable.run();
    }

    private void handlePassiveLocationAction(Intent intent) {
        if (intent.hasExtra(LocationManager.KEY_LOCATION_CHANGED)) {
            Location location = (Location) intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
            log.i("Passive location received: %s", location);
            if (location != null) {
                LocationData locationData = new LocationData(location, "passive", "passive");
                beaconTask.passiveRun(locationData);
            }
        } else {
            log.w("Passive location intent had no location data");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        createPersistentNotification();

        try {
            beaconTask = BeaconTask.create(this, executorService);
        } catch (IOException e) {
            log.e("Unable to create beacon task", e);
        }

        if (intent == null || ACTION_RUN_TASK.equals(intent.getAction())) {
            handleRunAction();
        } else if (ACTION_PASSIVE_LOCATION.equals(intent.getAction())) {
            handlePassiveLocationAction(intent);
            /* TODO For handleRunAction we should check the last run time
             *   if it was < our interval, schedule next for interval - how long ago last run time was  */
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (executorService != null) executorService.shutdown();
        if (configRepository != null)
            configRepository.unregisterOnSharedPreferenceChangeListener(configChangeListener);
        if (passiveLocationListener != null) passiveLocationListener.stopMonitoring();
    }

    private void createPersistentNotification() {
        Notifications notifications = new Notifications(this);
        ServiceCompat.startForeground(this,
                PERSISTENT_NOTIFICATION_ID,
                notifications.createPersistentNotification(
                        getString(R.string.persistent_notification_title),
                        configRepository.inHighAccuracyMode() ? getString(R.string.persistent_notification_high_accuracy_mode)
                                : getString(R.string.persistent_notification_message)
                ),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION : 0);
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
