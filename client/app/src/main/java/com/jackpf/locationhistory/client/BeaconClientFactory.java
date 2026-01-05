package com.jackpf.locationhistory.client;

import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.util.Logger;

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class BeaconClientFactory {
    private static final Logger log = new Logger("BeaconClientFactory");

    private static final int CLIENT_TIMEOUT_MILLIS = 10_000;

    public static BeaconClient createClient(ConfigRepository configRepo) {
        return createClient(configRepo, CLIENT_TIMEOUT_MILLIS);
    }

    public static BeaconClient createClient(ConfigRepository configRepo, int timeout) {
        log.d("Connecting to server %s:%d", configRepo.getServerHost(), configRepo.getServerPort());

        try {
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress(configRepo.getServerHost(), configRepo.getServerPort())
                    .idleTimeout(15, TimeUnit.SECONDS)
                    .keepAliveWithoutCalls(false)
                    .usePlaintext()
                    .build();

            return new BeaconClient(channel, timeout);
        } catch (IllegalArgumentException e) {
            log.e("Invalid server details", e);
            return null;
        }
    }

}
