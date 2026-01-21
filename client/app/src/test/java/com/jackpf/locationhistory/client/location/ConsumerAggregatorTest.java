package com.jackpf.locationhistory.client.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ConsumerAggregatorTest {

    @Test
    public void constructor_completesImmediately_whenZeroTasks() {
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<List<String>> result = new AtomicReference<>();

        new ConsumerAggregator<String>(0, list -> {
            completed.set(true);
            result.set(list);
        });

        assertTrue(completed.get());
        assertTrue(result.get().isEmpty());
    }

    @Test
    public void newChildConsumer_completesAfterSingleTask() {
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<List<String>> result = new AtomicReference<>();

        ConsumerAggregator<String> aggregator = new ConsumerAggregator<>(1, list -> {
            completed.set(true);
            result.set(list);
        });

        assertFalse(completed.get());

        Consumer<String> child = aggregator.newChildConsumer();
        child.accept("result1");

        assertTrue(completed.get());
        assertEquals(1, result.get().size());
        assertEquals("result1", result.get().get(0));
    }

    @Test
    public void newChildConsumer_completesOnlyAfterAllTasks() {
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<List<String>> result = new AtomicReference<>();

        ConsumerAggregator<String> aggregator = new ConsumerAggregator<>(3, list -> {
            completed.set(true);
            result.set(list);
        });

        Consumer<String> child1 = aggregator.newChildConsumer();
        Consumer<String> child2 = aggregator.newChildConsumer();
        Consumer<String> child3 = aggregator.newChildConsumer();

        child1.accept("first");
        assertFalse(completed.get());

        child2.accept("second");
        assertFalse(completed.get());

        child3.accept("third");
        assertTrue(completed.get());
        assertEquals(3, result.get().size());
    }

    @Test
    public void newChildConsumer_collectsAllResults() {
        AtomicReference<List<Integer>> result = new AtomicReference<>();

        ConsumerAggregator<Integer> aggregator = new ConsumerAggregator<>(3, result::set);

        aggregator.newChildConsumer().accept(10);
        aggregator.newChildConsumer().accept(20);
        aggregator.newChildConsumer().accept(30);

        assertEquals(3, result.get().size());
        assertTrue(result.get().contains(10));
        assertTrue(result.get().contains(20));
        assertTrue(result.get().contains(30));
    }

    @Test
    public void newChildConsumer_allowsNullValues() {
        AtomicReference<List<String>> result = new AtomicReference<>();

        ConsumerAggregator<String> aggregator = new ConsumerAggregator<>(3, result::set);

        aggregator.newChildConsumer().accept("value");
        aggregator.newChildConsumer().accept(null);
        aggregator.newChildConsumer().accept("another");

        assertEquals(3, result.get().size());
        assertTrue(result.get().contains("value"));
        assertTrue(result.get().contains(null));
        assertTrue(result.get().contains("another"));
    }

    @Test
    public void newChildConsumer_isThreadSafe() throws InterruptedException {
        int taskCount = 100;
        AtomicReference<List<Integer>> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        ConsumerAggregator<Integer> aggregator = new ConsumerAggregator<>(taskCount, list -> {
            result.set(list);
            latch.countDown();
        });

        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < taskCount; i++) {
            final int value = i;
            Consumer<Integer> child = aggregator.newChildConsumer();
            executor.submit(() -> child.accept(value));
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(taskCount, result.get().size());

        executor.shutdown();
    }

    @Test
    public void finalConsumer_calledExactlyOnce() {
        AtomicInteger callCount = new AtomicInteger(0);

        ConsumerAggregator<String> aggregator = new ConsumerAggregator<>(2, list -> callCount.incrementAndGet());

        aggregator.newChildConsumer().accept("a");
        aggregator.newChildConsumer().accept("b");

        assertEquals(1, callCount.get());
    }
}
