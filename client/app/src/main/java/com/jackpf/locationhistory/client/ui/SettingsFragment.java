package com.jackpf.locationhistory.client.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jackpf.locationhistory.client.R;
import com.jackpf.locationhistory.client.client.ssl.SSLPrompt;
import com.jackpf.locationhistory.client.config.ConfigRepository;
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
        sslPrompt = new SSLPrompt(getActivity());

        setupServerInputs();
        setupUpdateFrequency();
        setupUnifiedPush();
        observeEvents();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupServerInputs() {
        // Load saved values
        binding.serverHostInput.setText(viewModel.getConfig().getServerHost());
        binding.serverPortInput.setText(String.valueOf(viewModel.getConfig().getServerPort()));

        // Button handlers
        binding.testButton.setOnClickListener(v -> viewModel.testConnection(
                binding.serverHostInput.getText().toString(),
                binding.serverPortInput.getText().toString()
        ));

        binding.saveButton.setOnClickListener(v -> viewModel.saveSettings(
                binding.serverHostInput.getText().toString(),
                binding.serverPortInput.getText().toString(),
                (ConfigRepository.UpdateFrequency) binding.updateFrequencyInput.getTag(),
                binding.updateEveryInput.getText().toString()
        ));
    }

    private void setupUpdateFrequency() {
        String[] frequencyOptions = new String[]{
                getString(R.string.update_frequency_balanced),
                getString(R.string.update_frequency_high)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                frequencyOptions
        );
        binding.updateFrequencyInput.setAdapter(adapter);

        // Set initial values
        ConfigRepository.UpdateFrequency savedFrequency = viewModel.getConfig().getUpdateFrequency();
        binding.updateFrequencyInput.setTag(savedFrequency);
        binding.updateFrequencyInput.setText(frequencyOptions[savedFrequency.ordinal()], false);
        setUpdateEveryEnabled(savedFrequency == ConfigRepository.UpdateFrequency.SCHEDULED);

        binding.updateEveryInput.setText(String.valueOf(viewModel.getConfig().getUpdateIntervalMinutes()));

        // Listen for changes
        binding.updateFrequencyInput.setOnItemClickListener((parent, v, position, id) -> {
            ConfigRepository.UpdateFrequency frequency = ConfigRepository.UpdateFrequency.values()[position];
            binding.updateFrequencyInput.setTag(frequency);
            setUpdateEveryEnabled(frequency == ConfigRepository.UpdateFrequency.SCHEDULED);
        });
    }

    private void setUpdateEveryEnabled(boolean enabled) {
        binding.updateEveryInputLayout.setEnabled(enabled);
        binding.updateEveryInput.setEnabled(enabled);
    }

    private void setupUnifiedPush() {
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

            switch (event.type) {
                case TOAST:
                    int resId = (int) event.args[0];
                    if (event.args.length > 1)
                        Toasts.show(requireContext(), resId, (String) event.args[1]);
                    else Toasts.show(requireContext(), resId);
                    break;
                case SSL_PROMPT:
                    if (sslPrompt != null) sslPrompt.show((String) event.args[0], true);
                    break;
                case CHECK_UNIFIED_PUSH:
                    binding.pushRegisterSwitch.setChecked((Boolean) event.args[0]);
                    break;
                case SHOW_DISTRIBUTOR_PICKER:
                    showDistributorPicker((List<String>) event.args[0]);
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
