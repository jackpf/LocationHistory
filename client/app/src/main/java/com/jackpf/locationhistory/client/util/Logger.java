package com.jackpf.locationhistory.client.util;

import android.content.Context;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {
    private static final String EVENT_LOG_FILE = "event_log.txt";
    @Nullable
    private static File filesDir;

    private final String tag;

    public Logger(String tag) {
        this.tag = tag;
    }

    public Logger(Object context) {
        this.tag = context.getClass().getSimpleName();
    }

    /**
     * Needed for file logging
     */
    public static void initContext(Context context) {
        filesDir = context.getFilesDir();
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

    public void appendEventToFile(String msg, Object... args) {
        if (filesDir == null) {
            e("Logger not initialized - call initContext for file logging");
            return;
        }

        appendEventToFile(filesDir, msg, args);
    }

    public void appendEventToFile(File customFilesDir, String msg, Object... args) {
        File logFile = new File(customFilesDir, EVENT_LOG_FILE);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String entry = sdf.format(new Date()) + ": " + format(msg, args) + "\n";

        try (FileOutputStream fos = new FileOutputStream(logFile, true)) {
            fos.write(entry.getBytes());
        } catch (IOException e) {
            e("Logger", e);
        }
    }
}
