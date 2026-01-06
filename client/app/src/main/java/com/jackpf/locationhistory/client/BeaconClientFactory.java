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

    private static final int CLIENT_IDLE_TIMEOUT_MILLIS = 15_000;

    public static BeaconClient createClient(ConfigRepository configRepo) {
        return createClient(configRepo, CLIENT_TIMEOUT_MILLIS);
    }

    public static BeaconClient createClient(ConfigRepository configRepo, int timeout) {
        return createClient(
                configRepo.getServerHost(),
                configRepo.getServerPort(),
                timeout
        );
    }

    public static BeaconClient createClient(String host, int port, int timeout) {
        log.d("Connecting to server %s:%d", host, port);

        try {
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress(host, port)
                    .idleTimeout(CLIENT_IDLE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
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
