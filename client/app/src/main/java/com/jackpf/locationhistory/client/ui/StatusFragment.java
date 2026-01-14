package com.jackpf.locationhistory.client.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.PingResponse;
import com.jackpf.locationhistory.client.MainActivity;
import com.jackpf.locationhistory.client.R;
import com.jackpf.locationhistory.client.client.ssl.SSLPrompt;
import com.jackpf.locationhistory.client.client.ssl.UntrustedCertException;
import com.jackpf.locationhistory.client.client.util.GrpcFutureWrapper;
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.databinding.FragmentStatusBinding;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.util.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.annotation.Nullable;

public class StatusFragment extends Fragment {
    private FragmentStatusBinding binding;
    @Nullable
    private ConfigRepository configRepository;
    @Nullable
    private SSLPrompt sslPrompt;

    private final Logger log = new Logger(this);

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener =
            (sharedPreferences, key) -> updateUI();

    private final Handler heartbeatHandler = new Handler(Looper.getMainLooper());
    private static final long HEARTBEAT_INTERVAL_MS = 2000;
    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            updateConnectionAsync().addListener(
                    () -> heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS),
                    ContextCompat.getMainExecutor(requireContext())
            );
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        configRepository = new ConfigRepository(requireContext());
        configRepository.registerOnSharedPreferenceChangeListener(preferenceListener);

        sslPrompt = new SSLPrompt(getActivity());

        updateUI();

        heartbeatHandler.post(heartbeatRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (configRepository != null) {
            configRepository.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        }

        heartbeatHandler.removeCallbacks(heartbeatRunnable);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatusBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private ListenableFuture<PingResponse> testConnection() {
        if (getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).ping(GrpcFutureWrapper.empty());
        }
        return Futures.immediateFailedFuture(new IllegalStateException("No activity"));
    }

    private ListenableFuture<PingResponse> updateConnectionAsync() {
        ListenableFuture<PingResponse> testConnectionFuture = testConnection();

        Futures.addCallback(
                testConnectionFuture,
                new FutureCallback<PingResponse>() {
                    @Override
                    public void onSuccess(PingResponse result) {
                        if (BeaconClient.isPongResponse(result)) {
                            binding.statusTextView.setText(getString(R.string.connected));
                        } else {
                            binding.statusTextView.setText(getString(R.string.invalid_response));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        if (UntrustedCertException.isCauseOf(t)) {
                            sslPrompt.show(UntrustedCertException.getCauseFrom(t).getFingerprint(), false);
                        } else {
                            binding.statusTextView.setText(getString(R.string.disconnected));
                        }
                    }
                },
                ContextCompat.getMainExecutor(requireContext())
        );

        return testConnectionFuture;
    }

    private void updateUI() {
        if (getActivity() == null) {
            return;
        }

        log.d("Updating status fragment");

        getActivity().runOnUiThread(() -> {
            // Device status
            binding.deviceStateTextView.setText(configRepository.getDeviceStatus());

            // Last run time
            long lastRunTimestamp = configRepository.getLastRunTimestamp();
            if (lastRunTimestamp > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                binding.lastPingTextView.setText(sdf.format(new Date(lastRunTimestamp)));
            } else {
                binding.lastPingTextView.setText(getString(R.string.never));
            }
        });
    }
}
