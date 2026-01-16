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

import java.util.List;

public class PermissionsActivity extends AppCompatActivity {
    private LinearLayout rowsContainer;

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
        }
    }

    /**
     * Rebuilds table rows based on current permission status
     */
    private void updateUI() {
        List<AppRequirement> appRequirements = AppRequirements.getRequirements(this);

        if (AppRequirementsUtil.allGranted(this, appRequirements)) {
            finish();
            return;
        }

        rowsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (AppRequirement requirement : appRequirements) {
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
