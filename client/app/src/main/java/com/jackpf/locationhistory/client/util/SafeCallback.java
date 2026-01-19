package com.jackpf.locationhistory.client.util;

import android.content.Context;
import android.util.Log;

import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.work.ListenableWorker.Result;

import com.google.common.util.concurrent.FutureCallback;

import javax.annotation.Nullable;

public abstract class SafeCallback<T> implements FutureCallback<T> {
    private final Logger log = new Logger(this);

    private final Context context;
    @Nullable
    private final Completer<Result> completer;

    public SafeCallback(Context context, @Nullable Completer<Result> completer) {
        this.context = context;
        this.completer = completer;
    }

    public SafeCallback(Context context) {
        this(context, null);
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
        if (completer != null) completer.set(Result.failure());
    }
}
