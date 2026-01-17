package com.jackpf.locationhistory.client.client;

import lombok.Getter;
import lombok.Setter;

public abstract class PoolableClient implements AutoCloseable {
    @Getter
    @Setter
    private volatile boolean isPooled = false;

    protected abstract void shutdown();

    @Override
    public final void close() {
        if (isPooled) throw new IllegalStateException("Client is pooled and cannot be closed");
        else shutdown();
    }

    public final void forceClose() {
        setPooled(false);
        close();
    }
}
