package com.jackpf.locationhistory.client;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.jackpf.locationhistory.client.permissions.PermissionsFlow;
import com.jackpf.locationhistory.client.util.Log;

public class MainActivity extends Activity {
    private final PermissionsFlow permissionsFlow = new PermissionsFlow(this)
            .require(new String[]{Manifest.permission.ACCESS_FINE_LOCATION})
            .thenRequire(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION})
            .thenRequire(new String[]{Manifest.permission.POST_NOTIFICATIONS})
            .onComplete(() -> {
                Log.d("Starting foreground service");
                startForegroundService(new Intent(this, BeaconService.class));
            });

    private EditText serverBox;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        permissionsFlow.start();

        serverBox = findViewById(R.id.serverBox);

        SharedPreferences p = getSharedPreferences("beacon", MODE_PRIVATE);
        serverBox.setText(p.getString("server", ""));

        findViewById(R.id.saveBtn).setOnClickListener(v -> {
            p.edit().putString("server", serverBox.getText().toString()).apply();
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] permissions, @NonNull int[] grantResult) {
        super.onRequestPermissionsResult(code, permissions, grantResult);
        permissionsFlow.onRequestPermissionsResult(code, permissions, grantResult, deniedPermission -> {
            throw new RuntimeException("Permissions %s was denied".formatted(deniedPermission));
        });
    }
}
