package com.jackpf.locationhistory.client.client;

public class TestPoolableClient extends PoolableClient {
    private boolean shutdownCalled = false;

    public TestPoolableClient() {
    }

    public TestPoolableClient(String params) {
    }

    @Override
    protected void shutdown() {
        shutdownCalled = true;
    }

    public boolean wasShutdown() {
        return shutdownCalled;
    }

    public boolean isPooled() {
        try {
            close();
            return false;
        } catch (IllegalStateException e) {
            return true;
        }
    }
}
