package com.jackpf.locationhistory.client.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.jackpf.locationhistory.client.AppRequirements;
import com.jackpf.locationhistory.client.GrantPermissionsActivity;
import com.jackpf.locationhistory.client.databinding.FragmentPermissionsWarningBinding;
import com.jackpf.locationhistory.client.permissions.AppRequirementsUtil;

import javax.annotation.Nullable;

public class PermissionsWarningFragment extends Fragment {
    private FragmentPermissionsWarningBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPermissionsWarningBinding.inflate(inflater, container, false);

        binding.fixButton.setOnClickListener(view -> handleFixClick());

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean allRequirementGranted = AppRequirementsUtil.allGranted(requireContext(), AppRequirements.getRequirements(requireContext()));
        setPermissionsWarningVisibility(allRequirementGranted);
    }

    private void setPermissionsWarningVisibility(boolean allGranted) {
        View warningCard = binding.warningContainer;
        warningCard.setVisibility(allGranted ? View.GONE : View.VISIBLE);
    }

    private void handleFixClick() {
        startActivity(new Intent(requireContext(), GrantPermissionsActivity.class));
    }
}
