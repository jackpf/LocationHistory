package com.jackpf.locationhistory.client;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.grpc.util.GrpcFutureWrapper;
import com.jackpf.locationhistory.client.permissions.PermissionsFlow;
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;

public class MainActivity extends Activity {
    private ConfigRepository configRepo;
    private BeaconClient beaconClient;
    private EditText serverHostInput;
    private EditText serverPortInput;
    private EditText updateIntervalInput;
    private Button testButton;
    private Button saveButton;

    private final Logger log = new Logger(this);

    private final PermissionsFlow permissionsFlow = createPermissionsFlow();

    private static final long CLIENT_TIMEOUT_MILLIS = 10_000;

    private PermissionsFlow createPermissionsFlow() {
        PermissionsFlow permissionsFlow = new PermissionsFlow(this);

        permissionsFlow.require(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsFlow.thenRequire(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION});
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

        configRepo = new ConfigRepository(this);
        beaconClient = BeaconClientFactory.createClient(configRepo);

        setContentView(R.layout.activity_main);

        permissionsFlow.start();

        serverHostInput = findViewById(R.id.serverHostInput);
        serverPortInput = findViewById(R.id.serverPortInput);
        updateIntervalInput = findViewById(R.id.updateIntervalInput);
        testButton = findViewById(R.id.testButton);
        saveButton = findViewById(R.id.saveButton);

        serverHostInput.setText(configRepo.getServerHost());
        serverPortInput.setText(Integer.toString(configRepo.getServerPort()));
        updateIntervalInput.setText(Long.toString(configRepo.getUpdateIntervalMillis()));

        testButton.setOnClickListener(view -> handleTestClick());
        saveButton.setOnClickListener(view -> handleSaveClick());
    }


    private BeaconClient getBeaconClient() throws IOException {
        if (beaconClient != null && !beaconClient.isClosed()) return beaconClient;
        else throw new IOException("Client not connected");
    }

    private void handleTestClick() {
        try {
            getBeaconClient().ping(new GrpcFutureWrapper<>(
                    response -> runOnUiThread(() -> {
                        String responseMessage = response.getMessage();
                        if ("pong".equals(responseMessage)) {
                            Toast.makeText(this, "Connection successful", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, String.format("Invalid response message: %s", responseMessage), Toast.LENGTH_SHORT).show();
                        }
                    }),
                    e -> runOnUiThread(() -> Toast.makeText(this, String.format("Connection failed: %s", e.getMessage()), Toast.LENGTH_SHORT).show())
            ));
        } catch (IOException e) {
            Toast.makeText(this, String.format("Connection failed: %s", e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSaveClick() {
        configRepo.setServerHost(serverHostInput.getText().toString());
        configRepo.setServerPort(Integer.parseInt(serverPortInput.getText().toString()));
        configRepo.setUpdateIntervalMillis(Long.parseLong(updateIntervalInput.getText().toString()));
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        beaconClient.close();
    }
}
