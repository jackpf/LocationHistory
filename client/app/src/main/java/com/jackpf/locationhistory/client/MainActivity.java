package com.jackpf.locationhistory.client;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.PingResponse;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.grpc.util.GrpcFutureWrapper;
import com.jackpf.locationhistory.client.permissions.PermissionsFlow;
import com.jackpf.locationhistory.client.permissions.PermissionsManager;
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private ConfigRepository configRepo;
    private BeaconClient beaconClient;

    private final Logger log = new Logger(this);

    private PermissionsFlow permissionsFlow;

    private PermissionsFlow createPermissionsFlow() {
        PermissionsManager permissionsManager = new PermissionsManager(this);
        PermissionsFlow permissionsFlow = new PermissionsFlow(this);

        permissionsFlow.require(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsFlow.thenRequire(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION});
        }
        if (!permissionsManager.hasBackgroundPermission()) {
            permissionsFlow.requireSetting(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        }

        return permissionsFlow.onComplete(() -> {
            log.d("Permission flow complete");

            log.d("Starting beacon worker...");
            BeaconWorkerFactory.createWorker(this);
//            BeaconWorkerFactory.createTestWorker(this, 10, TimeUnit.SECONDS);
            log.d("Beacon worker started");
        });
    }

    public void refreshBeaconClient() {
        log.d("Refreshing beacon client");
        if (beaconClient != null && !beaconClient.isClosed()) beaconClient.close();
        try {
            beaconClient = BeaconClientFactory.createClient(configRepo);
        } catch (IOException e) {
            beaconClient = null;
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.activity_main);

        permissionsFlow = createPermissionsFlow();
        permissionsFlow.start();
    }


    @Override
    protected void onStart() {
        super.onStart();

        configRepo = new ConfigRepository(this);
        refreshBeaconClient();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (beaconClient != null) beaconClient.close();
    }

    private BeaconClient getBeaconClient() throws IOException {
        if (beaconClient != null && !beaconClient.isClosed()) return beaconClient;
        else throw new IOException("Client not connected");
    }

    public ListenableFuture<PingResponse> ping(GrpcFutureWrapper<PingResponse> callback) {
        try {
            return getBeaconClient().ping(callback);
        } catch (IOException e) {
            callback.onFailure(e);
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] permissions, @NonNull int[] grantResult) {
        super.onRequestPermissionsResult(code, permissions, grantResult);
        permissionsFlow.onRequestPermissionsResult(code, permissions, grantResult, deniedPermission -> {
            log.w("Permissions %s was denied", deniedPermission);
            Toast.makeText(this, getString(R.string.toast_permission_denied), android.widget.Toast.LENGTH_LONG).show();
            return false;
        });
    }
}
