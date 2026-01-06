package com.jackpf.locationhistory.client.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.jackpf.locationhistory.client.BeaconClientFactory;
import com.jackpf.locationhistory.client.BeaconWorkerFactory;
import com.jackpf.locationhistory.client.MainActivity;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.databinding.FragmentSettingsBinding;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.grpc.util.GrpcFutureWrapper;
import com.jackpf.locationhistory.client.util.Logger;

import javax.annotation.Nullable;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    @Nullable
    private ConfigRepository configRepository;

    private final Logger log = new Logger(this);

    @Override
    public void onResume() {
        super.onResume();

        configRepository = new ConfigRepository(requireContext());

        updateUI();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void updateUI() {
        log.d("Updating settings fragment");

        binding.serverHostInput.setText(configRepository.getServerHost());
        binding.serverPortInput.setText(Integer.toString(configRepository.getServerPort()));
        binding.updateIntervalInput.setText(Long.toString(configRepository.getUpdateIntervalMillis()));

        binding.testButton.setOnClickListener(view -> handleTestClick());
        binding.saveButton.setOnClickListener(view -> handleSaveClick());
    }

    private void handleTestClick() {
        // Create a temp client with current (non-saved) settings
        BeaconClient tempClient = BeaconClientFactory.createClient(
                binding.serverHostInput.getText().toString(),
                Integer.parseInt(binding.serverPortInput.getText().toString()),
                1500 // Smaller timeout for pings
        );

        tempClient.ping(new GrpcFutureWrapper<>(
                response -> getActivity().runOnUiThread(() -> {
                    String responseMessage = response.getMessage();
                    if ("pong".equals(responseMessage)) {
                        Toast.makeText(getActivity(), "Connection successful", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), String.format("Invalid response message: %s", responseMessage), Toast.LENGTH_SHORT).show();
                    }
                    tempClient.close();
                }),
                e -> getActivity().runOnUiThread(() -> {
                    Toast.makeText(getActivity(), String.format("Connection failed: %s", e.getMessage()), Toast.LENGTH_SHORT).show();
                    tempClient.close();
                })
        ));
    }

    private void handleSaveClick() {
        if (getActivity() instanceof MainActivity) {
            try {
                configRepository.setServerHost(binding.serverHostInput.getText().toString());
                configRepository.setServerPort(Integer.parseInt(binding.serverPortInput.getText().toString()));
                configRepository.setUpdateIntervalMillis(Long.parseLong(binding.updateIntervalInput.getText().toString()));

                MainActivity activity = (MainActivity) getActivity();
                activity.refreshBeaconClient();
                BeaconWorkerFactory.runOnce(getContext());

                Toast.makeText(getContext(), "Saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Invalid settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
