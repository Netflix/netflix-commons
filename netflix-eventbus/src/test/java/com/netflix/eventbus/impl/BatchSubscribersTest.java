package com.netflix.eventbus.impl;

import com.netflix.eventbus.spi.InvalidSubscriberException;
import com.netflix.eventbus.spi.Subscribe;
import com.netflix.eventbus.spi.SubscriberConfigProvider;
import junit.framework.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Nitesh Kant
 */
public class BatchSubscribersTest {

    @Test
    public void testBatch() throws Exception {
        EventBusImpl bus = new EventBusImpl();

        bus.setConsumerQueueSupplier(new EventBusImpl.ConsumerQueueSupplier() {
            @Override
            public ConsumerQueue get(Method subscriber, SubscriberConfigProvider.SubscriberConfig subscriberConfig, AtomicLong queueSizeCounter) {
                SizeAndAgeBatchingQueue q =
                        new SizeAndAgeBatchingQueue(subscriber, subscriberConfig, false, queueSizeCounter);
                return q;
            }
        });

        BatchConsumer subscriber = new BatchConsumer();
        bus.registerSubscriber(subscriber);

        for (int i = 0; i < 3; i++) {
            bus.publish("Hey buddy " + i);
        }
        synchronized (subscriber.mockReceiveMonitor) {
            subscriber.mockReceiveMonitor.wait(2000);
        }
        Assert.assertEquals("Not all events received by the consumer.", 3, subscriber.eventsCounter.get());
    }

    @Test
    public void testNestedGenericsRegistration() throws Exception {
        try {
            new EventBusImpl().registerSubscriber(new NestedGenericsBatchConsumer());
        } catch (InvalidSubscriberException e) {
            throw new AssertionError("Batch Subscriber with nested generic event failed to register.");
        }
    }

    @Test
    public void testNonTypedBatchSub() throws Exception {
        try {
            new EventBusImpl().registerSubscriber(new NoTypeBatchConsumer());
            throw new AssertionError("Batch Subscriber with no generic type successfully registered.");
        } catch (InvalidSubscriberException e) {
            // expected
        }
    }

    private class BatchConsumer {

        private final Object mockReceiveMonitor = new Object();
        private AtomicInteger eventsCounter = new AtomicInteger();

        @Subscribe(batchingStrategy = Subscribe.BatchingStrategy.SizeOrAge, batchSize = 2, batchAge = 60000)
        private void consume(Iterable<String> eventBatch) {
            for (String event : eventBatch) {
                eventsCounter.incrementAndGet();
            }
            synchronized (mockReceiveMonitor) {
                mockReceiveMonitor.notifyAll();
            }
        }
    }

    private class NestedGenericsBatchConsumer {

        @Subscribe(batchingStrategy = Subscribe.BatchingStrategy.SizeOrAge, batchSize = 2, batchAge = 60000)
        private void consumeComplex(Iterable<List<String>> eventBatch) {
        }
    }

    private class NoTypeBatchConsumer {

        @Subscribe(batchingStrategy = Subscribe.BatchingStrategy.SizeOrAge, batchSize = 2, batchAge = 60000)
        private void consumeComplex(Iterable eventBatch) {
        }
    }
}
