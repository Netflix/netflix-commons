package com.netflix.eventbus.impl;

import com.netflix.eventbus.spi.Subscribe;
import com.netflix.eventbus.spi.SubscriberConfigProvider;
import com.netflix.eventbus.utils.EventBusUtils;
import junit.framework.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Nitesh Kant (nkant@netflix.com)
 */
public class ConsumerQueueFullTest {

    @Test
    public void testQueueFull() throws Exception {
        BlockIndefinetly mySub = new BlockIndefinetly();
        Method subMethod = mySub.getClass().getMethod("subMe", String.class);
        MyConsumerQueueSupplier qSupplier = new MyConsumerQueueSupplier(false);
        EventConsumer consumer = new EventConsumer(subMethod, mySub, null, String.class, qSupplier);
        consumer.shutdown(); // Just to make the poller stop.
        qSupplier.resetCounters();

        for (int j=0; j < 5;j++) {
            consumer.enqueue("event" + j);
        }

        Assert.assertEquals("Unexpected queue offers ",            8, qSupplier.offers.get());
        Assert.assertEquals("Unexpected queue non blocking takes", 3, qSupplier.nonBlockingTake.get());
        Assert.assertEquals("Unexpected queue clear",              0, qSupplier.clear.get());

        Assert.assertEquals("Consumer retry stats not correct.", 3,
                consumer.getStats().QUEUE_OFFER_RETRY_COUNTER.getValue().longValue());
        Assert.assertEquals("Consumer queue size stats not correct.", 2, consumer.getStats().QUEUE_SIZE_COUNTER.get());

    }

    @Test
    public void testRetryFail() throws Exception {
        BlockIndefinetly mySub = new BlockIndefinetly();
        Method subMethod = mySub.getClass().getMethod("subMe", String.class);
        MyConsumerQueueSupplier qSupplier = new MyConsumerQueueSupplier(true);
        EventConsumer consumer = new EventConsumer(subMethod, mySub, null, String.class, qSupplier);
        consumer.shutdown(); // Just to make the poller stop.
        qSupplier.resetCounters();

        for (int j=0; j < 3;j++) {
            consumer.enqueue("event" + j);
        }

        Assert.assertEquals("Unexpected queue offers ", 8, qSupplier.offers.get());
        Assert.assertEquals("Unexpected queue non blocking takes", 5, qSupplier.nonBlockingTake.get());
        Assert.assertEquals("Unexpected queue clear", 0, qSupplier.clear.get());

        Assert.assertEquals("Consumer retry stats not correct.", 5,
                consumer.getStats().QUEUE_OFFER_RETRY_COUNTER.getValue().longValue());
        Assert.assertEquals("Consumer queue size stats not correct.", 2, consumer.getStats().QUEUE_SIZE_COUNTER.get());
        Assert.assertEquals("Consumer queue event rejected stats not correct.", 1,
                consumer.getStats().EVENT_ENQUEUE_REJECTED_COUNTER.getValue().longValue());

    }

    private static class MyConsumerQueueSupplier implements EventBusImpl.ConsumerQueueSupplier {

        private LinkedBlockingQueue q;
        private AtomicInteger offers = new AtomicInteger();
        private AtomicInteger nonBlockingTake = new AtomicInteger();
        private AtomicInteger blockingTake = new AtomicInteger();
        private AtomicInteger clear = new AtomicInteger();
        private boolean ignoreNonBlockingTakes;

        private MyConsumerQueueSupplier(boolean ignoreNonBlockingTakes) {
            this.ignoreNonBlockingTakes = ignoreNonBlockingTakes;
        }

        @Override
        public ConsumerQueue get(Method subscriberMethod, SubscriberConfigProvider.SubscriberConfig subscriberConfig,
                                 AtomicLong queueSizeCounter) {
            return new MyConsumerQueue(subscriberConfig, queueSizeCounter);
        }

        void resetCounters() {
            offers.set(0);
            nonBlockingTake.set(0);
            blockingTake.set(0);
            clear.set(0);
        }

        @SuppressWarnings("unchecked")
        private class MyConsumerQueue implements ConsumerQueue {

            private AtomicLong queueSizeCounter;

            private MyConsumerQueue(SubscriberConfigProvider.SubscriberConfig subscribe, AtomicLong queueSizeCounter) {
                this.queueSizeCounter = queueSizeCounter;
                q = new LinkedBlockingQueue(EventBusUtils.getQueueSize(subscribe));
            }

            @Override
            public boolean offer(Object event) {
                offers.incrementAndGet();
                if (q.offer(event)) {
                    queueSizeCounter.incrementAndGet();
                    return true;
                }
                return false;
            }

            @Override
            public Object nonBlockingTake() {
                nonBlockingTake.incrementAndGet();
                if (ignoreNonBlockingTakes) {
                    return null;
                }
                Object poll = q.poll();
                if (null != poll) {
                    queueSizeCounter.decrementAndGet();
                }
                return poll;
            }

            @Override
            public Object blockingTake() throws InterruptedException {
                blockingTake.incrementAndGet();
                Object take = q.take();
                queueSizeCounter.decrementAndGet();
                return take;
            }

            @Override
            public void clear() {
                clear.incrementAndGet();
                q.clear();
                queueSizeCounter.set(0);
            }
        }
    }

    public class BlockIndefinetly {

        private final Object dieWaiting = new Object();

        @Subscribe(queueSize = 2,batchingStrategy = Subscribe.BatchingStrategy.None)
        public void subMe(String event) {
            synchronized (dieWaiting) {
                try {
                    dieWaiting.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
