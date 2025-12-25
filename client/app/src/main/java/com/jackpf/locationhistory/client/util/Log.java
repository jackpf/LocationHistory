package com.jackpf.locationhistory.client.util;

public class Log {
    private static String getTag() {
        // Get the calling class (skips L class itself)
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement e : stack) {
            String cn = e.getClassName();
            if (!cn.equals(Log.class.getName()) && !cn.startsWith("java.lang.Thread")) {
                return cn.substring(cn.lastIndexOf('.') + 1);
            }
        }
        return "Unknown";
    }

    public static void d(String msg) {
        android.util.Log.d(getTag(), msg);
    }

    public static void i(String msg) {
        android.util.Log.i(getTag(), msg);
    }

    public static void w(String msg) {
        android.util.Log.w(getTag(), msg);
    }

    public static void e(String msg) {
        android.util.Log.e(getTag(), msg);
    }

    public static void e(String msg, Throwable t) {
        android.util.Log.e(getTag(), msg, t);
    }

    public static void v(String msg) {
        android.util.Log.v(getTag(), msg);
    }
}
