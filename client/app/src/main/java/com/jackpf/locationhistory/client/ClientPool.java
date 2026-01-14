package com.jackpf.locationhistory.client;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;

import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;

import lombok.AllArgsConstructor;
import lombok.Data;

public class ClientPool<P, C> {
    @Data
    @AllArgsConstructor
    public static class ClientInfo<C> {
        C client;
        Runnable close;
    }

    public interface ClientFactory<P, C> {
        C create(P params) throws IOException;
    }

    private final Logger log = new Logger(this);

    private final LruCache<P, ClientInfo<C>> pool;

    public ClientPool(int maxSize) {
        pool = new LruCache<P, ClientInfo<C>>(maxSize) {
            @Override
            protected void entryRemoved(boolean evicted, @NonNull P key, @NonNull ClientInfo<C> oldValue, ClientInfo<C> newValue) {
                try {
                    oldValue.close.run();
                } catch (Exception e) {
                    log.e("Error closing evicted client", e);
                }
            }
        };
    }

    public synchronized C getOrCreate(P params, ClientFactory<P, ClientInfo<C>> clientFactory) throws IOException {
        ClientInfo<C> clientInfo = pool.get(params);

        if (clientInfo == null) {
            clientInfo = clientFactory.create(params);
            if (clientInfo != null) pool.put(params, clientInfo);
        }

        return clientInfo != null ? clientInfo.client : null;
    }
}
