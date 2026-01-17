package com.jackpf.locationhistory.client.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class ClientPoolTest {

    private ClientPool<String, TestPoolableClient> pool;

    @Before
    public void setUp() {
        pool = new ClientPool<>(2);
    }

    @Test
    public void getOrCreate_createsNewClientWhenPoolEmpty() throws IOException {
        TestPoolableClient client = pool.getOrCreate("key1", TestPoolableClient::new);

        assertNotNull(client);
        assertTrue(client.isPooled());
    }

    @Test
    public void getOrCreate_returnsCachedClientForSameKey() throws IOException {
        TestPoolableClient client1 = pool.getOrCreate("key1", TestPoolableClient::new);
        TestPoolableClient client2 = pool.getOrCreate("key1", TestPoolableClient::new);

        assertSame(client1, client2);
    }

    @Test
    public void getOrCreate_createsDifferentClientsForDifferentKeys() throws IOException {
        TestPoolableClient client1 = pool.getOrCreate("key1", TestPoolableClient::new);
        TestPoolableClient client2 = pool.getOrCreate("key2", TestPoolableClient::new);

        assertNotNull(client1);
        assertNotNull(client2);
        assertTrue(client1 != client2);
    }

    @Test
    public void getOrCreate_evictsOldestClientWhenPoolFull() throws IOException {
        TestPoolableClient client1 = pool.getOrCreate("key1", TestPoolableClient::new);
        pool.getOrCreate("key2", TestPoolableClient::new);
        pool.getOrCreate("key3", TestPoolableClient::new);

        // client1 should have been evicted and closed
        assertTrue(client1.wasShutdown());
    }

    @Test
    public void getOrCreate_factoryOnlyCalledOnce() throws IOException {
        AtomicInteger factoryCalls = new AtomicInteger(0);
        ClientPool.ClientFactory<String, TestPoolableClient> factory = params -> {
            factoryCalls.incrementAndGet();
            return new TestPoolableClient(params);
        };

        pool.getOrCreate("key1", factory);
        pool.getOrCreate("key1", factory);
        pool.getOrCreate("key1", factory);

        assertEquals(1, factoryCalls.get());
    }

    @Test
    public void getOrCreate_factoryReturnsNull_doesNotCacheNull() throws IOException {
        AtomicInteger factoryCalls = new AtomicInteger(0);
        ClientPool.ClientFactory<String, TestPoolableClient> factory = params -> {
            factoryCalls.incrementAndGet();
            return null;
        };

        pool.getOrCreate("key1", factory);
        pool.getOrCreate("key1", factory);

        assertEquals(2, factoryCalls.get());
    }
}
