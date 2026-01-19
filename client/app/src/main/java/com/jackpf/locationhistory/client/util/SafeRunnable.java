package com.jackpf.locationhistory.client.util;

import android.content.Context;
import android.util.Log;

import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.work.ListenableWorker.Result;

public class SafeRunnable implements Runnable {
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
            FileLogger.appendLog(context, "WORKER INIT CRASH: " + Log.getStackTraceString(t));
            completer.set(Result.failure());
        }
    }
}
