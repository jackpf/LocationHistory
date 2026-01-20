package com.jackpf.locationhistory.client.location;

import android.Manifest;
import android.location.LocationManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresPermission;

import com.jackpf.locationhistory.client.util.Logger;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class OptimisedProvider implements LocationProvider {
    private final Logger log = new Logger(this);

    private final LocationManager locationManager;
    private final ExecutorService threadExecutor;

    public OptimisedProvider(LocationManager locationManager,
                             ExecutorService threadExecutor) {
        this.locationManager = locationManager;
        this.threadExecutor = threadExecutor;
    }

    public boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    @Override
    public void provide(String source, int timeout, Consumer<LocationData> consumer) {
        if (!isSupported())
            throw new RuntimeException("getLiveLocation requires Android R or above");
        CancellationSignal cancellationSignal = new CancellationSignal();
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable onTimeout = () -> {
            cancellationSignal.cancel();
            consumer.accept(null);
        };
        handler.postDelayed(onTimeout, timeout);

        locationManager.getCurrentLocation(
                source,
                cancellationSignal,
                threadExecutor,
                location -> {
                    handler.removeCallbacks(onTimeout);
                    
                    if (location != null) consumer.accept(new LocationData(location, source));
                    else consumer.accept(null);
                }
        );
    }
}
