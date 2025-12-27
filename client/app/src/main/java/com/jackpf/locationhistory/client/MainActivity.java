package com.jackpf.locationhistory.client;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.permissions.PermissionsFlow;
import com.jackpf.locationhistory.client.util.Logger;

public class MainActivity extends Activity {
    private ConfigRepository configRepository;
    private BeaconService beaconService;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            beaconService = ((BeaconService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            beaconService = null;
        }
    };

    private EditText serverHostInput;
    private EditText serverPortInput;
    private EditText updateIntervalInput;
    private Button testButton;
    private Button saveButton;

    private final Logger log = new Logger(this);

    private final PermissionsFlow permissionsFlow = createPermissionsFlow();

    private PermissionsFlow createPermissionsFlow() {
        PermissionsFlow permissionsFlow = new PermissionsFlow(this);

        permissionsFlow.require(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsFlow.thenRequire(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION});
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsFlow.thenRequire(new String[]{Manifest.permission.POST_NOTIFICATIONS});
        }

        return permissionsFlow.onComplete(() -> {
            log.d("Starting foreground service");
            startForegroundService(new Intent(this, BeaconService.class));
        });
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        configRepository = new ConfigRepository(this);
        setContentView(R.layout.activity_main);

        permissionsFlow.start();

        serverHostInput = findViewById(R.id.serverHostInput);
        serverPortInput = findViewById(R.id.serverPortInput);
        updateIntervalInput = findViewById(R.id.updateIntervalInput);
        testButton = findViewById(R.id.testButton);
        saveButton = findViewById(R.id.saveButton);

        serverHostInput.setText(configRepository.getServerHost());
        serverPortInput.setText(Integer.toString(configRepository.getServerPort()));
        updateIntervalInput.setText(Long.toString(configRepository.getUpdateIntervalMillis()));

        testButton.setOnClickListener(view -> handleTestClick());
        saveButton.setOnClickListener(view -> handleSaveClick());
    }

    private void handleTestClick() {
        if (beaconService == null) {
            Toast.makeText(this, "Service not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = beaconService.testConnection();
        String message = success ? "Connection successful" : "Connection failed";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleSaveClick() {
        configRepository.setServerHost(serverHostInput.getText().toString());
        configRepository.setServerPort(Integer.parseInt(serverPortInput.getText().toString()));
        configRepository.setUpdateIntervalMillis(Long.parseLong(updateIntervalInput.getText().toString()));
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] permissions, @NonNull int[] grantResult) {
        super.onRequestPermissionsResult(code, permissions, grantResult);
        permissionsFlow.onRequestPermissionsResult(code, permissions, grantResult, deniedPermission -> {
            throw new RuntimeException(String.format("Permissions %s was denied", deniedPermission));
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, BeaconService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
        beaconService = null;
    }
}
