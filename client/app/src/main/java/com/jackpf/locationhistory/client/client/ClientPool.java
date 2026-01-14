package com.jackpf.locationhistory.client.client;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;

import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;

public class ClientPool<P, C extends PoolableClient> {
    public interface ClientFactory<P, C extends PoolableClient> {
        C create(P params) throws IOException;
    }

    private final Logger log = new Logger(this);

    private final LruCache<P, C> pool;

    public ClientPool(int maxSize) {
        pool = new LruCache<P, C>(maxSize) {
            @Override
            protected void entryRemoved(boolean evicted, @NonNull P key, @NonNull C oldValue, C newValue) {
                try {
                    oldValue.forceClose();
                } catch (Exception e) {
                    log.e("Error closing evicted client", e);
                }
            }
        };
    }

    public synchronized C getOrCreate(P params, ClientFactory<P, C> clientFactory) throws IOException {
        C client = pool.get(params);

        if (client == null) {
            client = clientFactory.create(params);
            if (client != null) pool.put(params, client);
        }

        return client;
    }
}
