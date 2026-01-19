package com.jackpf.locationhistory.client;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;

import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.ui.Notifications;
import com.jackpf.locationhistory.client.worker.BeaconWorkerFactory;

import java.util.concurrent.TimeUnit;

public class BeaconService extends Service {
    private HandlerThread handlerThread;
    private Handler handler;
    private ConfigRepository configRepository;
    private static final int PERSISTENT_NOTIFICATION_ID = 1;

    private final Runnable locationRunner = new Runnable() {
        @Override
        public void run() {
            try {
                BeaconWorkerFactory.runOnce(BeaconService.this);
            } finally {
                handler.postDelayed(this, TimeUnit.MINUTES.toMillis(configRepository.getUpdateIntervalMinutes()));
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handlerThread = new HandlerThread("ServiceTimer");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        configRepository = new ConfigRepository(this);
        Notifications notifications = new Notifications(this);

        ServiceCompat.startForeground(this,
                PERSISTENT_NOTIFICATION_ID,
                notifications.createPersistentNotification("", "Beacon active"),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION : 0);

        handler.removeCallbacks(locationRunner);
        handler.post(locationRunner);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (handler != null) handler.removeCallbacks(locationRunner);
        if (handlerThread != null) handlerThread.quitSafely();
        super.onDestroy();
    }
}
