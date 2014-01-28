package com.netflix.eventbus.impl;

import com.netflix.eventbus.spi.SubscriberConfigProvider;
import com.netflix.eventbus.utils.EventBusUtils;

import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The default implementation for {@link com.netflix.eventbus.impl.EventBusImpl.ConsumerQueueSupplier}. Our
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
class DefaultConsumerQueueSupplier implements EventBusImpl.ConsumerQueueSupplier {

    @Override
    public ConsumerQueue get(Method subscriber, final SubscriberConfigProvider.SubscriberConfig subscriberConfig, final AtomicLong queueSizeCounter) {
        switch (subscriberConfig.getBatchingStrategy()) {
            case Age:
                return new AgeBatchingQueue(subscriber, subscriberConfig, queueSizeCounter);
            case SizeOrAge:
                return new SizeAndAgeBatchingQueue(subscriber, subscriberConfig, queueSizeCounter);
        }
        return new ConsumerQueue() {

            private LinkedBlockingQueue delegate = new LinkedBlockingQueue(EventBusUtils.getQueueSize(subscriberConfig));

            @Override
            @SuppressWarnings("unchecked")
            public boolean offer(Object event) {
                boolean offered = delegate.offer(event);
                if(offered) {
                    queueSizeCounter.incrementAndGet();
                }
                return offered;
            }

            @Override
            public Object nonBlockingTake() {
                Object retrievedItem = delegate.poll();
                if (null != retrievedItem) {
                    queueSizeCounter.decrementAndGet();
                }
                return retrievedItem;
            }

            @Override
            public Object blockingTake() throws InterruptedException {
                Object retrieved = delegate.take();
                queueSizeCounter.decrementAndGet();
                return retrieved;
            }

            @Override
            public void clear() {
                delegate.clear();
                queueSizeCounter.set(0);
            }
        };
    }
}
