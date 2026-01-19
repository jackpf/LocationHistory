package com.jackpf.locationhistory.client.util;

import android.content.Context;
import android.util.Log;

import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.work.ListenableWorker.Result;

public class SafeRunnable implements Runnable {
    private final Logger log = new Logger(this);

    private final Completer<Result> completer;
    private final Context context;
    private final ThrowingRunnable task;

    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    public SafeRunnable(Completer<Result> completer, Context context, ThrowingRunnable task) {
        this.completer = completer;
        this.context = context;
        this.task = task;
    }

    @Override
    public void run() {
        try {
            task.run();
        } catch (Throwable t) {
            handleCrash(t);
        }
    }

    public static void runStatic()

    private void handleCrash(Throwable t) {
        log.e("Runnable error", t);
        log.appendEventToFile(context, "Runnable error: %s", Log.getStackTraceString(t));
        completer.set(Result.failure());
    }
}
