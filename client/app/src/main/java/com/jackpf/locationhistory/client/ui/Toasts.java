package com.jackpf.locationhistory.client.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class Toasts {
    private Toasts() {
    }

    public static void show(Context context, int resId, Object... formatArgs) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, context.getString(resId, formatArgs), Toast.LENGTH_SHORT).show());
    }
}
