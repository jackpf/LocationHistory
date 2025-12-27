package com.jackpf.locationhistory.client.util;

import java.util.Locale;

public class Logger {
    private final String tag;

    public Logger(String tag) {
        this.tag = tag;
    }

    public Logger(Object context) {
        this.tag = context.getClass().getSimpleName();
    }

    private String format(String msg, Object... args) {
        if (args != null && args.length > 0) {
            try {
                return String.format(Locale.getDefault(), msg, args);
            } catch (Exception e) {
                return msg + " [Format Error: " + e.getMessage() + "]";
            }
        }
        return msg;
    }

    public void d(String msg, Object... args) {
        android.util.Log.d(tag, format(msg, args));
    }

    public void i(String msg, Object... args) {
        android.util.Log.i(tag, format(msg, args));
    }

    public void w(String msg, Object... args) {
        android.util.Log.w(tag, format(msg, args));
    }

    public void v(String msg, Object... args) {
        android.util.Log.v(tag, format(msg, args));
    }

    public void e(String msg, Object... args) {
        android.util.Log.e(tag, format(msg, args));
    }

    public void e(String msg, Throwable t) {
        android.util.Log.e(tag, msg, t);
    }

    public void e(Throwable t, String msg, Object... args) {
        android.util.Log.e(tag, format(msg, args), t);
    }
}
