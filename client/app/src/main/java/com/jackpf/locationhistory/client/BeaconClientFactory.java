package com.jackpf.locationhistory.client;

import com.jackpf.locationhistory.client.config.ConfigRepository;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.ssl.DynamicTrustManager;
import com.jackpf.locationhistory.client.ssl.TrustedCertStorage;
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;

public class BeaconClientFactory {
    private static final Logger log = new Logger("BeaconClientFactory");

    private static final int CLIENT_TIMEOUT_MILLIS = 10_000;

    private static final int CLIENT_IDLE_TIMEOUT_MILLIS = 15_000;

    private static OkHttpChannelBuilder secureChannel(
            OkHttpChannelBuilder builder,
            DynamicTrustManager trustManager,
            String host
    ) throws IOException {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());

            return builder
                    .sslSocketFactory(sslContext.getSocketFactory())
                    .overrideAuthority(host);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.e(e, "Error creating secure channel");
            throw new IOException("Error creating secure channel", e);
        }
    }

    public static BeaconClient createClient(ConfigRepository configRepo, boolean waitForReady, TrustedCertStorage storage) throws IOException {
        return createClient(configRepo, waitForReady, CLIENT_TIMEOUT_MILLIS, storage);
    }

    public static BeaconClient createClient(
            ConfigRepository configRepo,
            boolean waitForReady,
            int timeout,
            TrustedCertStorage storage
    ) throws IOException {
        return createClient(
                configRepo.getServerHost(),
                configRepo.getServerPort(),
                waitForReady,
                timeout,
                storage
        );
    }

    public static BeaconClient createClient(
            String host,
            int port,
            boolean waitForReady,
            int timeout,
            TrustedCertStorage storage
    ) throws IOException {
        log.d("Connecting to server %s:%d", host, port);

        try {
            OkHttpChannelBuilder builder = OkHttpChannelBuilder
                    .forAddress(host, port)
                    .idleTimeout(CLIENT_IDLE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                    .keepAliveWithoutCalls(false);
            DynamicTrustManager dynamicTrustManager = new DynamicTrustManager(storage);
            builder = secureChannel(builder, dynamicTrustManager, host);
            ManagedChannel channel = builder.build();

            return new BeaconClient(channel, dynamicTrustManager, waitForReady, timeout);
        } catch (IllegalArgumentException e) {
            log.e("Invalid server details", e);
            throw new IOException("Invalid server details", e);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            log.e(e, "Error creating dynamic trust manager");
            throw new IOException("Error creating dynamic trust manager", e);
        }
    }
}
