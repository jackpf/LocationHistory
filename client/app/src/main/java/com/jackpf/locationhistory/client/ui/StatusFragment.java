package com.jackpf.locationhistory.client.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.jackpf.locationhistory.client.client.ssl.SSLPrompt;
import com.jackpf.locationhistory.client.databinding.FragmentStatusBinding;

import javax.annotation.Nullable;

public class StatusFragment extends Fragment {
    private FragmentStatusBinding binding;
    private StatusViewModel viewModel;

    @Nullable
    private SSLPrompt sslPrompt;

    private final Handler heartbeatHandler = new Handler(Looper.getMainLooper());
    private static final long HEARTBEAT_INTERVAL_MS = 2000;
    private final Runnable heartbeatRunnable = new Runnable() { // TODO In viewModel?
        @Override
        public void run() {
            viewModel.checkConnection().addListener(
                    () -> heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS),
                    ContextCompat.getMainExecutor(requireContext())
            );
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatusBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StatusViewModel.class);
        sslPrompt = new SSLPrompt(getActivity());

        observeEvents();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.startListening();
        heartbeatHandler.post(heartbeatRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        viewModel.stopListening();
        heartbeatHandler.removeCallbacks(heartbeatRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void observeEvents() {
        viewModel.getEvents().observe(getViewLifecycleOwner(), event -> {
            if (event == null) return;

            switch (event.getType()) {
                case UPDATE_CONNECTION_STATUS:
                    StatusViewEvent.UpdateConnectionStatus statusEvent = (StatusViewEvent.UpdateConnectionStatus) event;
                    binding.statusTextView.setText(statusEvent.getStatusResId());
                    break;
                case UPDATE_DEVICE_STATE:
                    StatusViewEvent.UpdateDeviceState uiEvent = (StatusViewEvent.UpdateDeviceState) event;
                    binding.deviceStateTextView.setText(uiEvent.getDeviceState());
                    binding.lastPingTextView.setText(uiEvent.getLastPing());
                    break;
                case SHOW_SSL_PROMPT:
                    StatusViewEvent.ShowSslPrompt sslEvent = (StatusViewEvent.ShowSslPrompt) event;
                    if (sslPrompt != null) sslPrompt.show(sslEvent.getFingerprint(), false);
                    break;
            }
        });
    }
}
