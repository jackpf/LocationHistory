package com.jackpf.locationhistory.client.client;

import com.jackpf.locationhistory.client.client.ssl.DynamicTrustManager;
import com.jackpf.locationhistory.client.client.ssl.TrustedCertStorage;
import com.jackpf.locationhistory.client.grpc.BeaconClient;
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.grpc.ManagedChannel;
import lombok.AllArgsConstructor;
import lombok.Data;

// TODO Make non-static
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

    /**
     * Creates an un-pooled client
     * Clients created with this method should be manually closed
     */
    public static BeaconClient createClient(BeaconClientParams params,
                                            TrustedCertStorage storage) throws IOException {
        try {
            DynamicTrustManager dynamicTrustManager = new DynamicTrustManager(storage);
            ManagedChannel channel = SecureChannel.create(
                    params.getHost(),
                    params.getPort(),
                    DEFAULT_CLIENT_IDLE_TIMEOUT,
                    false,
                    dynamicTrustManager
            );
            ExecutorService threadExecutor = Executors.newSingleThreadExecutor();

            return new BeaconClient(
                    channel,
                    threadExecutor,
                    dynamicTrustManager,
                    params.isWaitForReady(),
                    params.getTimeout()
            );
        } catch (IllegalArgumentException e) {
            log.e("Invalid server details", e);
            throw new IOException("Invalid server details", e);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            log.e(e, "Error creating dynamic trust manager");
            throw new IOException("Error creating dynamic trust manager", e);
        }
    }

    /**
     * Creates a long-lived pooled client
     * This client is shared across the application, so shouldn't be manually closed
     */
    public static BeaconClient createPooledClient(
            BeaconClientParams clientParams,
            TrustedCertStorage storage
    ) throws IOException {
        return clientPool.getOrCreate(
                clientParams,
                params -> {
                    log.d("Connecting to server %s:%d", params.getHost(), params.getPort());
                    return createClient(params, storage);
                }
        );
    }
}
