package com.jackpf.locationhistory.client.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jackpf.locationhistory.client.R;
import com.jackpf.locationhistory.client.client.ssl.SSLPrompt;
import com.jackpf.locationhistory.client.databinding.FragmentSettingsBinding;
import com.jackpf.locationhistory.client.push.ObservableUnifiedPushState;
import com.jackpf.locationhistory.client.util.Logger;

import java.util.List;

import javax.annotation.Nullable;

public class SettingsFragment extends Fragment {
    private final Logger log = new Logger(this);

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;

    @Nullable
    private SSLPrompt sslPrompt;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        sslPrompt = new SSLPrompt(requireActivity());

        setupInputs();
        setupUnifiedPushListener();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupInputs() {
        binding.serverHostInput.setText(viewModel.getConfig().getServerHost());
        binding.serverPortInput.setText(String.valueOf(viewModel.getConfig().getServerPort()));
        binding.updateFrequencyInput.setText(Integer.toString(viewModel.getConfig().getUpdateIntervalMinutes()));

        binding.testButton.setOnClickListener(v -> viewModel.testConnection(
                binding.serverHostInput.getText().toString(),
                binding.serverPortInput.getText().toString()
        ));

        binding.saveButton.setOnClickListener(v -> viewModel.saveSettings(
                binding.serverHostInput.getText().toString(),
                binding.serverPortInput.getText().toString(),
                binding.updateFrequencyInput.getText().toString()
        ));
    }

    private void setupUnifiedPushListener() {
        ObservableUnifiedPushState.getInstance(requireContext())
                .observeEnabled()
                .observe(getViewLifecycleOwner(), isEnabled -> {
                    if (binding.pushRegisterSwitch.isChecked() != isEnabled) {
                        binding.pushRegisterSwitch.setOnCheckedChangeListener(null);
                        binding.pushRegisterSwitch.setChecked(isEnabled);
                    }
                    binding.pushRegisterSwitch.setOnCheckedChangeListener((v, isChecked) ->
                            viewModel.handleUnifiedPushToggle(requireContext(), isChecked)
                    );
                });
    }

    private void observeEvents() {
        viewModel.getEvents().observe(getViewLifecycleOwner(), event -> {
            if (event == null) return;

            switch (event.getType()) {
                case TOAST:
                    SettingsViewEvent.Toast toast = (SettingsViewEvent.Toast) event;
                    Toasts.show(requireContext(), toast.getMessageResId(), toast.getFormatArgs());
                    break;
                case SSL_PROMPT:
                    SettingsViewEvent.SslPrompt sslPromptEvent = (SettingsViewEvent.SslPrompt) event;
                    if (sslPrompt != null) sslPrompt.show(sslPromptEvent.getFingerprint(), true);
                    break;
                case SET_UNIFIED_PUSH_CHECKED:
                    SettingsViewEvent.SetUnifiedPushChecked checkedEvent = (SettingsViewEvent.SetUnifiedPushChecked) event;
                    binding.pushRegisterSwitch.setChecked(checkedEvent.isChecked());
                    break;
                case SHOW_DISTRIBUTOR_PICKER:
                    SettingsViewEvent.ShowDistributorPicker pickerEvent = (SettingsViewEvent.ShowDistributorPicker) event;
                    showDistributorPicker(pickerEvent.getDistributors());
                    break;
            }
        });
    }

    private void showDistributorPicker(List<String> distributors) {
        String[] distributorsArray = distributors.toArray(new String[0]);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.select_distributor)
                .setItems(distributorsArray, (dialog, which) ->
                        viewModel.registerUnifiedPush(requireContext(), distributorsArray[which])
                )
                .setNegativeButton(R.string.cancel, (dialog, which) ->
                        binding.pushRegisterSwitch.setChecked(false)
                )
                .setOnCancelListener(dialog ->
                        binding.pushRegisterSwitch.setChecked(false)
                )
                .show();
    }
}
