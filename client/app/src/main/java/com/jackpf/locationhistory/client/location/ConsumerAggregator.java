package com.jackpf.locationhistory.client.location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ConsumerAggregator<T> {
    private final int totalTasks;
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final List<T> results;
    private final Consumer<List<T>> finalConsumer;

    public ConsumerAggregator(int totalTasks, Consumer<List<T>> finalConsumer) {
        this.totalTasks = totalTasks;
        this.finalConsumer = finalConsumer;
        this.results = Collections.synchronizedList(new ArrayList<>(totalTasks));

        // If no tasks, complete instantly
        if (totalTasks == 0) {
            finalConsumer.accept(results);
        }
    }

    public Consumer<T> newChildConsumer() {
        return (result) -> {
            results.add(result);
            int currentCount = completedCount.incrementAndGet();

            if (currentCount == totalTasks) {
                finalConsumer.accept(results);
            }
        };
    }
}
