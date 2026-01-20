package com.jackpf.locationhistory.client;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.ui.Notifications;
import com.jackpf.locationhistory.client.util.Logger;
import com.jackpf.locationhistory.client.worker.BeaconResult;
import com.jackpf.locationhistory.client.worker.BeaconTask;
import com.jackpf.locationhistory.client.worker.RetryableException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BeaconService extends Service {
    private final Logger log = new Logger(this);
    private HandlerThread handlerThread;
    private Handler handler;
    private ExecutorService executorService;
    private ConfigRepository configRepository;
    private static final int PERSISTENT_NOTIFICATION_ID = 1;

    private static final String START_MESSAGE = "Beacon task started";
    private static final String SUCCESS_MESSAGE = "Beacon task completed successfully";
    private static final String FAILED_MESSAGE = "Beacon task failed";
    private static final String RETRY_MESSAGE = "Beacon task failed with retry";

    private final Runnable beaconTaskRunner = () -> {
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
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener configChangeListener = (prefs, key) -> {
        if (key != null &&
                (key.equals(ConfigRepository.SERVER_HOST_KEY)
                        || key.equals(ConfigRepository.SERVER_PORT_KEY)
                        || key.equals(ConfigRepository.UPDATE_INTERVAL_KEY))) {
            log.d("Detected config change");
            scheduleNext(500); // Debounce config changes with 500ms delay
        }
    };

    private void scheduleNext(long delayMillis) {
        log.d("Scheduling next beacon task in %dms", delayMillis);

        handler.removeCallbacks(beaconTaskRunner);
        handler.postDelayed(beaconTaskRunner, delayMillis);
    }

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

        handlerThread = new HandlerThread("ServiceTimer");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        executorService = Executors.newSingleThreadExecutor();

        configRepository = new ConfigRepository(this);
        configRepository.registerOnSharedPreferenceChangeListener(configChangeListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Notifications notifications = new Notifications(this);

        ServiceCompat.startForeground(this,
                PERSISTENT_NOTIFICATION_ID,
                notifications.createPersistentNotification(getString(R.string.persistent_notification_title), getString(R.string.persistent_notification_message)),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION : 0);

        handler.removeCallbacks(beaconTaskRunner);
        handler.post(beaconTaskRunner);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (handler != null) handler.removeCallbacks(beaconTaskRunner);
        if (handlerThread != null) handlerThread.quitSafely();
        if (executorService != null) executorService.shutdown();
        if (configRepository != null)
            configRepository.unregisterOnSharedPreferenceChangeListener(configChangeListener);
    }
}
