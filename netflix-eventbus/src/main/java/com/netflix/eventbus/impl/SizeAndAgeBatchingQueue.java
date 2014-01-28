package com.netflix.eventbus.impl;

import com.google.common.annotations.VisibleForTesting;
import com.netflix.eventbus.spi.SubscriberConfigProvider;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Extends the {@link AgeBatchingQueue} to add one more reaping point based on the current batch size.
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
class SizeAndAgeBatchingQueue extends AgeBatchingQueue {

    private final int batchSize;

    SizeAndAgeBatchingQueue(Method subscriber, SubscriberConfigProvider.SubscriberConfig subscribe, AtomicLong queueSizeCounter) {
        this(subscriber, subscribe, true, queueSizeCounter);
    }

    @VisibleForTesting
    SizeAndAgeBatchingQueue(Method subscriber, SubscriberConfigProvider.SubscriberConfig subscribe, boolean scheduleReaper,
                            AtomicLong queueSizeCounter) {
        super(subscriber, subscribe, scheduleReaper, queueSizeCounter);
        batchSize = subscribe.getBatchSize();
    }

    @Override
    protected AgeBatch createNewBatch(@Nullable SubscriberConfigProvider.SubscriberConfig subscribe) {
        return new AgeAndSizeBatch((null != subscribe) ? subscribe.getBatchSize() : batchSize);
    }

    private class AgeAndSizeBatch extends AgeBatch {

        private final int batchSize;
        private AtomicInteger currentBatchSize;

        protected AgeAndSizeBatch(int batchSize) {
            super();
            this.batchSize = batchSize;
            currentBatchSize = new AtomicInteger();
        }

        @Override
        protected boolean addEvent(Object event) {
            if(currentBatchSize.get() >= batchSize) {
                if(!reapCurrentBatch("Batch size exceeded")) {
                    return false;
                }
            }
            if(super.addEvent(event)) {
                currentBatchSize.incrementAndGet();
                return true;
            }
            return false;
        }

        @Override
        protected void clear() {
            super.clear();
            currentBatchSize.set(0);
        }
    }
}
