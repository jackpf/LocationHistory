package com.jackpf.locationhistory.client.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.PingResponse;
import com.jackpf.locationhistory.client.MainActivity;
import com.jackpf.locationhistory.client.R;
import com.jackpf.locationhistory.client.client.BeaconClientFactory;
import com.jackpf.locationhistory.client.client.ssl.SSLPrompt;
import com.jackpf.locationhistory.client.client.ssl.TrustedCertStorage;
import com.jackpf.locationhistory.client.client.ssl.UntrustedCertException;
import com.jackpf.locationhistory.client.client.util.GrpcFutureWrapper;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.databinding.FragmentSettingsBinding;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.push.Ntfy;
import com.jackpf.locationhistory.client.push.ObservableUnifiedPushState;
import com.jackpf.locationhistory.client.push.UnifiedPushService;
import com.jackpf.locationhistory.client.util.Logger;

import org.unifiedpush.android.connector.UnifiedPush;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    @Nullable
    private ConfigRepository configRepository;
    @Nullable
    private SSLPrompt sslPrompt;

    private final Logger log = new Logger(this);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        configRepository = new ConfigRepository(requireContext());
        sslPrompt = new SSLPrompt(getActivity());

        setupUpdateFrequency();
        setupUnifiedPushListener();
    }

    @Override
    public void onResume() {
        super.onResume();

        binding.serverHostInput.setText(configRepository.getServerHost());
        binding.serverPortInput.setText(Integer.toString(configRepository.getServerPort()));

        binding.testButton.setOnClickListener(view -> handleTestClick());
        binding.saveButton.setOnClickListener(view -> handleSaveClick());
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

    private void handleTestClick() {
        if (getActivity() == null) {
            return;
        }

        try {
            // Create a temp client with current (non-saved) settings
            BeaconClient tempClient = BeaconClientFactory.createClient(
                    new BeaconClientFactory.BeaconClientParams(
                            binding.serverHostInput.getText().toString(),
                            Integer.parseInt(binding.serverPortInput.getText().toString()),
                            false,
                            3000 // Smaller timeout for pings
                    ),
                    new TrustedCertStorage(getActivity())
            );

            ListenableFuture<PingResponse> pingResponse = tempClient.ping(new GrpcFutureWrapper<>(
                    response -> getActivity().runOnUiThread(() -> {
                        if (BeaconClient.isPongResponse(response)) {
                            Toasts.show(getActivity(), R.string.toast_connection_successful);
                        } else {
                            Toasts.show(getActivity(), R.string.toast_invalid_response, response.getMessage());
                        }
                    }),
                    e -> {
                        if (UntrustedCertException.isCauseOf(e)) {
                            sslPrompt.show(UntrustedCertException.getCauseFrom(e).getFingerprint(), true);
                        } else {
                            requireActivity().runOnUiThread(() ->
                                    Toasts.show(getActivity(), R.string.toast_connection_failed, e.getMessage())
                            );
                        }
                    }
            ));

            pingResponse.addListener(tempClient::close, ContextCompat.getMainExecutor(requireContext()));
        } catch (NumberFormatException | IOException e) {
            Toasts.show(requireContext(), R.string.toast_invalid_settings, e.getMessage());
        }
    }

    private void handleSaveClick() {
        if (getActivity() instanceof MainActivity) {
            try {
                configRepository.setServerHost(binding.serverHostInput.getText().toString());
                configRepository.setServerPort(Integer.parseInt(binding.serverPortInput.getText().toString()));

                // Save update frequency settings
                String selectedFrequency = binding.updateFrequencyInput.getText().toString();
                int frequencyValue = selectedFrequency.equals(getString(R.string.update_frequency_high))
                        ? ConfigRepository.UPDATE_FREQUENCY_HIGH
                        : ConfigRepository.UPDATE_FREQUENCY_BALANCED;
                configRepository.setUpdateFrequency(frequencyValue);

                // Save update interval if high frequency is selected
                if (frequencyValue == ConfigRepository.UPDATE_FREQUENCY_HIGH) {
                    String intervalText = binding.updateEveryInput.getText().toString();
                    if (!intervalText.isEmpty()) {
                        configRepository.setUpdateIntervalMinutes(Integer.parseInt(intervalText));
                    }
                }

                Toasts.show(requireContext(), R.string.toast_saved);
            } catch (NumberFormatException e) {
                Toasts.show(requireContext(), R.string.toast_invalid_settings, e.getMessage());
            }
        }
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

        int savedFrequency = configRepository.getUpdateFrequency();
        binding.updateFrequencyInput.setText(frequencyOptions[savedFrequency], false);
        setUpdateEveryEnabled(savedFrequency == ConfigRepository.UPDATE_FREQUENCY_HIGH);
        binding.updateFrequencyInput.setOnItemClickListener((parent, v, position, id) -> {
            boolean enabled = position == ConfigRepository.UPDATE_FREQUENCY_HIGH;
            setUpdateEveryEnabled(enabled);
        });

        int savedUpdateInterval = configRepository.getUpdateIntervalMinutes();
        binding.updateEveryInput.setText(String.valueOf(savedUpdateInterval));
    }

    private void setUpdateEveryEnabled(boolean enabled) {
        binding.updateEveryInputLayout.setEnabled(enabled);
        binding.updateEveryInput.setEnabled(enabled);
    }

    private void setupUnifiedPushListener() {
        isUnifiedPushEnabled().observe(getViewLifecycleOwner(), isEnabled -> {
            // Prevent infinite loops
            if (binding.pushRegisterSwitch.isChecked() != isEnabled) {
                binding.pushRegisterSwitch.setOnCheckedChangeListener(null);
                binding.pushRegisterSwitch.setChecked(isEnabled);
            }
            binding.pushRegisterSwitch.setOnCheckedChangeListener((v, isChecked) -> handleUnifiedPushCheck(isChecked));
        });
    }

    private LiveData<Boolean> isUnifiedPushEnabled() {
        return ObservableUnifiedPushState
                .getInstance(requireContext())
                .observeEnabled();
    }

    private void handleUnifiedPushCheck(boolean isChecked) {
        if (getActivity() instanceof MainActivity) {
            if (isChecked) {
                List<String> distributors = UnifiedPush.getDistributors(requireContext());
                if (!distributors.isEmpty()) {
                    log.d("Found distributors: %s", Arrays.toString(distributors.toArray()));
                    // TODO Select distributor properly from list
                    UnifiedPushService.register(requireContext(), distributors.get(0));
                } else {
                    log.d("No push distributors");
                    Ntfy.promptInstall(requireContext());
                    binding.pushRegisterSwitch.setChecked(false);
                }
            } else {
                UnifiedPushService.unregister(requireContext());
            }
        }
    }
}
