package com.jackpf.locationhistory.client.util;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileLogger {
    private static final Logger log = new Logger("FileLogger");
    private static final String DEBUG_FILE = "debug_log.txt";

    public static void appendLog(Context context, String text) {
        File logFile = new File(context.getFilesDir(), DEBUG_FILE);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String entry = timestamp + ": " + text + "\n";

        try (FileOutputStream fos = new FileOutputStream(logFile, true)) {
            fos.write(entry.getBytes());
        } catch (IOException e) {
            log.e("FileLogger", e);
        }
    }
}
