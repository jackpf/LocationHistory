package com.jackpf.locationhistory.client.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.PingResponse;
import com.jackpf.locationhistory.client.BeaconClientFactory;
import com.jackpf.locationhistory.client.BeaconWorkerFactory;
import com.jackpf.locationhistory.client.MainActivity;
import com.jackpf.locationhistory.client.R;
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
import com.jackpf.locationhistory.client.push.UnifiedPushStorage;
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

    @Override
    public void onResume() {
        super.onResume();

        configRepository = new ConfigRepository(requireContext());
        sslPrompt = new SSLPrompt(getActivity());

        if (Boolean.TRUE.equals(isUnifiedPushEnabled().getValue())) {
            binding.pushRegisterSwitch.setChecked(true);
        }

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Listen to async changes of unified push enabled state
        isUnifiedPushEnabled().observe(getViewLifecycleOwner(), isEnabled -> {
            // Prevent infinite loops
            if (binding.pushRegisterSwitch.isChecked() != isEnabled) {
                binding.pushRegisterSwitch.setOnCheckedChangeListener(null);
                binding.pushRegisterSwitch.setChecked(isEnabled);
                binding.pushRegisterSwitch.setOnCheckedChangeListener((v, isChecked) -> handleUnifiedPushCheck(isChecked));
            }
        });
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

        binding.testButton.setOnClickListener(view -> handleTestClick());
        binding.saveButton.setOnClickListener(view -> handleSaveClick());
        binding.pushRegisterSwitch.setOnCheckedChangeListener((v, isChecked) -> handleUnifiedPushCheck(isChecked));
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

                MainActivity activity = (MainActivity) getActivity();
                activity.refreshBeaconClient();
                BeaconWorkerFactory.runOnce(requireContext());

                Toasts.show(requireContext(), R.string.toast_saved);
            } catch (NumberFormatException e) {
                Toasts.show(requireContext(), R.string.toast_invalid_settings, e.getMessage());
            }
        }
    }

    private LiveData<Boolean> isUnifiedPushEnabled() {
        UnifiedPushStorage unifiedPushStorage = new UnifiedPushStorage(requireContext());
        return ObservableUnifiedPushState
                .getInstance(requireActivity(), unifiedPushStorage)
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
