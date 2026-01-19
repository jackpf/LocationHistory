package com.jackpf.locationhistory.client.util;

import android.content.Context;
import android.util.Log;

import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.work.ListenableWorker.Result;

import com.google.common.util.concurrent.FutureCallback;

public abstract class SafeCallback<T> implements FutureCallback<T> {
    private final Logger log = new Logger(this);
    private final Completer<Result> completer;
    private final Context context;

    public SafeCallback(Completer<Result> completer, Context context) {
        this.completer = completer;
        this.context = context;
    }

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
        log.e("Callback exception", t);
        log.appendEventToFile(context, "Callback exception: %s", Log.getStackTraceString(t));
        completer.set(Result.failure());
    }
}
