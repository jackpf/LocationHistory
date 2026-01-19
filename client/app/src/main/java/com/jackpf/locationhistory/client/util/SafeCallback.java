package com.jackpf.locationhistory.client.util;

import android.content.Context;
import android.util.Log;

import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.work.ListenableWorker.Result;

import com.google.common.util.concurrent.FutureCallback;

public abstract class SafeCallback<T> implements FutureCallback<T> {
    private final Completer<Result> completer;
    private final Context context; // For logging

    public SafeCallback(Completer<Result> completer, Context context) {
        this.completer = completer;
        this.context = context;
    }

    // You implement this instead of onSuccess
    public abstract void onSafeSuccess(T result) throws Exception;

    @Override
    public final void onSuccess(T result) {
        try {
            onSafeSuccess(result);
        } catch (Throwable t) {
            handleCrash(t);
        }
    }

    private void handleCrash(Throwable t) {
        FileLogger.appendLog(context, "WORKER FAILURE: " + Log.getStackTraceString(t));
        // Ensure the worker doesn't hang!
        completer.set(Result.failure());
    }
}
