package com.jackpf.locationhistory.client.push;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Ntfy {
    private static final String NTFY_PACKAGE = "io.heckel.ntfy";

    public static void promptInstall(Context context) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + NTFY_PACKAGE)));
        } catch (ActivityNotFoundException e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + NTFY_PACKAGE)));
        }
    }
}
