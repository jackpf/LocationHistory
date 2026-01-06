package com.jackpf.locationhistory.client.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
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

    @Override
    public void onResume() {
        super.onResume();

        configRepository = new ConfigRepository(requireContext());
        configRepository.registerOnSharedPreferenceChangeListener(preferenceListener);

        updateUI();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (configRepository != null) {
            configRepository.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        }
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
        return Futures.immediateFuture(null);
    }

    private void updateUI() {
        log.d("Updating status fragment");

        // TODO This needs to happen after client is refreshed
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
                    public void onFailure(Throwable t) {
                        binding.statusTextView.setText("disconnected");
                    }
                },
                ContextCompat.getMainExecutor(requireContext()));

        getActivity().runOnUiThread(() -> {
            // 1. Time
            long lastRun = 123L;//repo.getLastRunTime();
            if (lastRun > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                binding.lastPingTextView.setText(sdf.format(new Date(lastRun)));
            } else {
                binding.lastPingTextView.setText("Never");
            }

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
