package com.jackpf.locationhistory.client.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppExecutors {
    private static final AppExecutors INSTANCE = new AppExecutors();

    private final ExecutorService background;
    private final Executor mainThread;

    private AppExecutors() {
        this.background = Executors.newCachedThreadPool();
        this.mainThread = new MainThreadExecutor();
    }

    public static AppExecutors getInstance() {
        return INSTANCE;
    }

    public ExecutorService background() {
        return background;
    }

    public Executor mainThread() {
        return mainThread;
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}
