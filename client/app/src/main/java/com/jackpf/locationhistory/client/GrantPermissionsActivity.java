package com.jackpf.locationhistory.client;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jackpf.locationhistory.client.permissions.AppRequirement;
import com.jackpf.locationhistory.client.permissions.AppRequirementsUtil;

import java.util.ArrayList;
import java.util.List;

public class GrantPermissionsActivity extends AppCompatActivity {
    private LinearLayout rowsContainer;
    private final List<AppRequirement> registeredAppRequirements = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        rowsContainer = findViewById(R.id.rowsContainer);

        setupPermissionsList();
        updateUI();
    }

    private void setupPermissionsList() {
        List<AppRequirement> requirements = AppRequirements.getRequirements(this);
        for (AppRequirement requirement : requirements) {
            requirement.register(this, this::updateUI);
            registeredAppRequirements.add(requirement);
        }
    }

    /**
     * Rebuilds table rows based on current permission status
     */
    private void updateUI() {
        if (AppRequirementsUtil.allGranted(this, registeredAppRequirements)) {
            finish();
            return;
        }

        rowsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (AppRequirement requirement : registeredAppRequirements) {
            View row = inflater.inflate(R.layout.activity_permissions_item, rowsContainer, false);
            boolean isGranted = requirement.isGranted(this);

            TextView descriptionText = row.findViewById(R.id.descriptionLabel);
            TextView subDescriptionText = row.findViewById(R.id.subDescriptionLabel);
            Button grantButton = row.findViewById(R.id.grantButton);
            View isGrantedLabel = row.findViewById(R.id.grantedLabel);

            descriptionText.setText(requirement.getDescription());
            subDescriptionText.setText(requirement.getExplanation());

            if (isGranted) {
                grantButton.setVisibility(View.GONE);
                isGrantedLabel.setVisibility(View.VISIBLE);
            } else {
                grantButton.setVisibility(View.VISIBLE);
                isGrantedLabel.setVisibility(View.GONE);

                grantButton.setOnClickListener(v -> requirement.request(this));
            }

            rowsContainer.addView(row);
        }
    }
}
