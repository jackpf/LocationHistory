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
import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.databinding.FragmentStatusBinding;
import com.jackpf.locationhistory.client.grpc.util.GrpcFutureWrapper;
import com.jackpf.locationhistory.client.util.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.annotation.Nullable;

public class StatusFragment extends Fragment {
    private FragmentStatusBinding binding;
    @Nullable
    private ConfigRepository configRepository;

    private final Logger log = new Logger(this);

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener =
            (sharedPreferences, key) -> updateUI();

    private final Handler heartbeatHandler = new Handler(Looper.getMainLooper());
    private static final long HEARTBEAT_INTERVAL_MS = 2000;
    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            updateConnectionAsync();

            heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        configRepository = new ConfigRepository(requireContext());
        configRepository.registerOnSharedPreferenceChangeListener(preferenceListener);

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

    private void updateConnectionAsync() {
        Futures.addCallback(
                testConnection(),
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(PingResponse result) {
                        if (result.getMessage().equals("pong")) {
                            binding.statusTextView.setText("connected");
                        } else {
                            binding.statusTextView.setText("invalid response");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        binding.statusTextView.setText("disconnected");
                    }
                },
                ContextCompat.getMainExecutor(requireContext())
        );
    }

    private void updateUI() {
        log.d("Updating status fragment");

        getActivity().runOnUiThread(() -> {
            // Last run time
            long lastRunTimestamp = configRepository.getLastRunTimestamp();
            if (lastRunTimestamp > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                binding.lastPingTextView.setText(sdf.format(new Date(lastRunTimestamp)));
            } else {
                binding.lastPingTextView.setText("never");
            }

            // Device status
            binding.deviceStateTextView.setText(configRepository.getDeviceStatus());

//            // 2. Result
//            String error = repo.getLastError();
//            if (error != null) {
//                binding.tvResultValue.setText(error);
//                binding.tvResultValue.setTextColor(Color.RED);
//                binding.tvStatus.setText("Error");
//            } else {
//                binding.tvResultValue.setText(repo.getLastLocation());
//                binding.tvResultValue.setTextColor(Color.BLACK);
//                binding.tvStatus.setText("Healthy");
//            }
        });
    }
}
