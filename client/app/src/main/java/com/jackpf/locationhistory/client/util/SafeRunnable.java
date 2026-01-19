package com.jackpf.locationhistory.client.util;

import android.content.Context;
import android.util.Log;

import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.work.ListenableWorker.Result;

import javax.annotation.Nullable;

public class SafeRunnable implements Runnable {
    private final Logger log = new Logger(this);

    private final Context context;
    @Nullable
    private final Completer<Result> completer;
    private final ThrowingRunnable task;

    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    public SafeRunnable(Context context, @Nullable Completer<Result> completer, ThrowingRunnable task) {
        this.context = context;
        this.completer = completer;
        this.task = task;
    }

    public SafeRunnable(Context context, ThrowingRunnable task) {
        this(context, null, task);
    }

    @Override
    public void run() {
        try {
            task.run();
        } catch (Throwable t) {
            handleCrash(t);
        }
    }

    private void handleCrash(Throwable t) {
        log.e("Runnable error", t);
        log.appendEventToFile(context, "Runnable error: %s", Log.getStackTraceString(t));
        if (completer != null) completer.set(Result.failure());
    }
}
