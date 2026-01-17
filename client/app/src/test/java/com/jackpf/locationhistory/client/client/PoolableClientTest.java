package com.jackpf.locationhistory.client.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class PoolableClientTest {

    @Test
    public void close_whenNotPooled_callsShutdown() {
        TestPoolableClient client = new TestPoolableClient();

        client.close();

        assertTrue(client.wasShutdown());
    }

    @Test
    public void close_whenPooled_throwsIllegalStateException() {
        TestPoolableClient client = new TestPoolableClient();
        client.setPooled(true);

        try {
            client.close();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("pooled"));
        }

        assertFalse(client.wasShutdown());
    }

    @Test
    public void forceClose_whenPooled_closesAnyway() {
        TestPoolableClient client = new TestPoolableClient();
        client.setPooled(true);

        client.forceClose();

        assertTrue(client.wasShutdown());
    }

    @Test
    public void forceClose_whenNotPooled_closesNormally() {
        TestPoolableClient client = new TestPoolableClient();

        client.forceClose();

        assertTrue(client.wasShutdown());
    }

    @Test
    public void close_afterForceClose_stillWorks() {
        TestPoolableClient client = new TestPoolableClient();
        client.setPooled(true);

        client.forceClose();

        // Should not throw since forceClose sets isPooled to false
        assertTrue(client.wasShutdown());
    }
}
