package com.jackpf.locationhistory.client;

import com.jackpf.locationhistory.client.ssl.DynamicTrustManager;
import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;

public class SecureChannel {
    private static final Logger log = new Logger("SecureChannel");

    private SecureChannel() {
    }

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

    public static ManagedChannel create(
            String host,
            int port,
            int idleTimeoutMs,
            boolean keepAliveWithoutCalls,
            DynamicTrustManager trustManager
    ) throws IOException {
        OkHttpChannelBuilder builder = OkHttpChannelBuilder
                .forAddress(host, port)
                .idleTimeout(idleTimeoutMs, TimeUnit.MILLISECONDS)
                .keepAliveWithoutCalls(keepAliveWithoutCalls);

        return secureChannel(builder, trustManager, host)
                .build();
    }
}
