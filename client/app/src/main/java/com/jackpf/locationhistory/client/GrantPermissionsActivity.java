package com.jackpf.locationhistory.client;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.jackpf.locationhistory.client.permissions.AppRequirement;
import com.jackpf.locationhistory.client.permissions.AppRequirementsUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Value;

public class GrantPermissionsActivity extends AppCompatActivity {
    private PermissionsAdapter adapter;
    private final List<AppRequirement> registeredAppRequirements = new ArrayList<>();

    /**
     * Wrapper class to hold AppRequirement and its granted state for diffing
     */
    @Value
    private static class PermissionItem {
        AppRequirement requirement;
        boolean isGranted;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        adapter = new PermissionsAdapter(this);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

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
     * Updates the RecyclerView with current permission status
     */
    private void updateUI() {
        if (AppRequirementsUtil.allGranted(this, registeredAppRequirements)) {
            finish();
            return;
        }

        List<PermissionItem> items = new ArrayList<>();
        for (AppRequirement requirement : registeredAppRequirements) {
            items.add(new PermissionItem(requirement, requirement.isGranted(this)));
        }
        adapter.submitList(items);
    }

    private static class PermissionsAdapter extends ListAdapter<PermissionItem, PermissionsAdapter.ViewHolder> {
        private final Context context;

        private static final DiffUtil.ItemCallback<PermissionItem> diffCallback =
                new DiffUtil.ItemCallback<PermissionItem>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull PermissionItem oldItem, @NonNull PermissionItem newItem) {
                        return Objects.equals(oldItem.requirement.getName(), newItem.requirement.getName());
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull PermissionItem oldItem, @NonNull PermissionItem newItem) {
                        return oldItem.equals(newItem);
                    }
                };

        PermissionsAdapter(Context context) {
            super(diffCallback);
            this.context = context;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.activity_permissions_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PermissionItem item = getItem(position);
            holder.bind(item, context);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView descriptionText;
            private final TextView subDescriptionText;
            private final Button grantButton;
            private final View grantedLabel;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                descriptionText = itemView.findViewById(R.id.descriptionLabel);
                subDescriptionText = itemView.findViewById(R.id.subDescriptionLabel);
                grantButton = itemView.findViewById(R.id.grantButton);
                grantedLabel = itemView.findViewById(R.id.grantedLabel);
            }

            void bind(PermissionItem item, Context context) {
                descriptionText.setText(item.requirement.getDescription());
                subDescriptionText.setText(item.requirement.getExplanation());

                if (item.isGranted) {
                    grantButton.setVisibility(View.GONE);
                    grantedLabel.setVisibility(View.VISIBLE);
                } else {
                    grantButton.setVisibility(View.VISIBLE);
                    grantedLabel.setVisibility(View.GONE);
                    grantButton.setOnClickListener(v -> item.requirement.request(context));
                }
            }
        }
    }
}
