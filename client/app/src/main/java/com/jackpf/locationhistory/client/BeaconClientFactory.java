package com.jackpf.locationhistory.client;

import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.ssl.DynamicTrustManager;
import com.jackpf.locationhistory.client.ssl.TrustedCertStorage;
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;

public class BeaconClientFactory {
    private static final Logger log = new Logger("BeaconClientFactory");

    private static final int CLIENT_POOL_SIZE = 10;
    public static final int DEFAULT_TIMEOUT = 10_000;
    private static final int DEFAULT_CLIENT_IDLE_TIMEOUT = 15_000;

    @Data
    @AllArgsConstructor
    public static class BeaconClientParams {
        String host;
        int port;
        boolean waitForReady;
        int timeout;
    }

    private static final ClientPool<BeaconClientParams, BeaconClient> clientPool = new ClientPool<>(
            CLIENT_POOL_SIZE
    );

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

    /**
     * Creates a long-lived static client
     * This client is shared across the application, so shouldn't be manually closed
     */
    public static BeaconClient createClient(
            BeaconClientParams clientParams,
            TrustedCertStorage storage
    ) throws IOException {
        return clientPool.getOrCreate(
                clientParams,
                params -> {
                    log.d("Connecting to server %s:%d", params.getHost(), params.getPort());

                    OkHttpChannelBuilder builder = OkHttpChannelBuilder
                            .forAddress(params.getHost(), params.getPort())
                            .idleTimeout(DEFAULT_CLIENT_IDLE_TIMEOUT, TimeUnit.MILLISECONDS)
                            .keepAliveWithoutCalls(false);

                    try {
                        DynamicTrustManager dynamicTrustManager = new DynamicTrustManager(storage);
                        builder = secureChannel(builder, dynamicTrustManager, params.getHost());
                        ManagedChannel channel = builder.build();
                        ExecutorService threadExecutor = Executors.newSingleThreadExecutor();

                        BeaconClient newClient = new BeaconClient(channel, threadExecutor, dynamicTrustManager, params.isWaitForReady(), params.getTimeout());

                        return new ClientPool.ClientInfo<>(
                                newClient,
                                () -> {
                                    threadExecutor.shutdown();
                                    channel.shutdown();
                                    dynamicTrustManager.close();
                                }
                        );
                    } catch (IllegalArgumentException e) {
                        log.e("Invalid server details", e);
                        throw new IOException("Invalid server details", e);
                    } catch (NoSuchAlgorithmException | KeyStoreException e) {
                        log.e(e, "Error creating dynamic trust manager");
                        throw new IOException("Error creating dynamic trust manager", e);
                    }
                }
        );
    }
}
