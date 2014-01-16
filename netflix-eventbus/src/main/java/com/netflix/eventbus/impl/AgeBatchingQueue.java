package com.netflix.eventbus.impl;

import com.google.common.annotations.VisibleForTesting;
import com.netflix.eventbus.spi.Subscribe;
import com.netflix.eventbus.spi.SubscriberConfigProvider;
import com.netflix.eventbus.utils.EventBusUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of {@link Subscribe.BatchingStrategy#Age} for {@link EventBusImpl}. The following is the strategy and
 * nuances of this implementation:
 * <ul>
 * <li>This queue maintains a current batch, an instance of {@link AgeBatch}</li>
 * <li>All calls to {@link AgeBatchingQueue#offer(Object)} will add the event to this batch.</li>
 * <li>All batches which are aged (crossed the max age) move to a blocking queue.</li>
 * <li>All age based batching subscribers share a single {@link Timer} to deduce the batch age periodically.</li>
 * <li>All individual instances of this queue will schedule a single task in the above timer to deduce the batch age
 * according to the batch age specified in {@link Subscribe}</li>
 * <li>The above task will periodically move the current batch to the old batches queue, mentioned above.</li>
 * <li>In case, the old batch queue is full, the reaper task sets a flag signifying that the queue is full and does
 * <em>NOT</em> reap the current batch.</li>
 * <li>Every subsequent offer to this queue, will try to reap the current batch, failing which, the offer will fail.</li>
 * <li>The failure of above offer will typically make the consumer remove & discard a batch and retry.</li>
 </ul>
 * @author Nitesh Kant (nkant@netflix.com)
 */
class AgeBatchingQueue implements EventBusImpl.ConsumerQueueSupplier.ConsumerQueue {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AgeBatchingQueue.class);

    protected AtomicReference<AgeBatch> currentBatch;
    protected LinkedBlockingQueue<AgeBatch> oldBatches;
    protected AtomicBoolean oldBatchesQueueFull;

    protected ReentrantLock batchReapingLock;
    /**
     * This IS a static timer. This is solely used for the purpose of routinely reaping the current batch to the old
     * batches queue. The tasks will ALWAYS use offer on the old batches queue and if it can not enqueue will leave the
     * current batch as is. After that any subsequent offer will first offer the current batch to the old queue, which if
     * fails, will fail the offer. So, in a nutshell, these timer tasks must be super quick and never block. So, it is
     * fine to even schedule thousands of these task (i.e. thousands of aged/size & age consumers) to this timer.
     */
    protected static Timer batchAgeChecker = new Timer("eventbus-consumer-current-batch-reaper", true);
    protected final String subscriberName;
    protected TimerTask reaper;
    protected Subscribe.BatchingStrategy batchingStrategy;
    protected AtomicLong queueSizeCounter;


    AgeBatchingQueue(Method subscriber, SubscriberConfigProvider.SubscriberConfig subscribe, AtomicLong queueSizeCounter) {
        this(subscriber, subscribe, true, queueSizeCounter);
    }

    @VisibleForTesting
    AgeBatchingQueue(Method subscriber, SubscriberConfigProvider.SubscriberConfig subscribe, boolean scheduleReaper,
                     AtomicLong queueSizeCounter) {
        this.queueSizeCounter = queueSizeCounter;
        subscriberName = subscriber.toGenericString();
        batchingStrategy = subscribe.getBatchingStrategy();
        oldBatches = new LinkedBlockingQueue<AgeBatch>(EventBusUtils.getQueueSize(subscribe));
        currentBatch = new AtomicReference<AgeBatch>(createNewBatch(subscribe));
        oldBatchesQueueFull = new AtomicBoolean();
        batchReapingLock = new ReentrantLock();
        int batchAge = subscribe.getBatchAge();
        reaper = new ReaperTask();
        // For testing we do not schedule a reaper but invoke reaping at will to have more predictability.
        if (scheduleReaper) {
            batchAgeChecker.schedule(reaper, batchAge, batchAge);
        }
    }

    @Override
    public boolean offer(Object event) {
        if (oldBatchesQueueFull.get()) {
            if (!reapCurrentBatch("Offering Thread")) {
                return false;
            }
        }
        return currentBatch.get().addEvent(event);
    }

    @Override
    public Object nonBlockingTake() {
        AgeBatch batch = oldBatches.poll();
        if (null != batch) {
            queueSizeCounter.decrementAndGet();
        }
        return batch;
    }

    @Override
    public Object blockingTake() throws InterruptedException {
        AgeBatch batch = oldBatches.take();
        queueSizeCounter.decrementAndGet();
        return batch;
    }

    @Override
    public void clear() {
        oldBatches.clear();
        currentBatch.get().clear();
        queueSizeCounter.set(0);
    }

    @VisibleForTesting
    AgeBatch getCurrentBatch() {
        return currentBatch.get();
    }

    @VisibleForTesting
    AgeBatch blockingTakeWithTimeout(long timeoutInMillis) throws InterruptedException {
        return oldBatches.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
    }

    @VisibleForTesting
    boolean invokeReaping() {
        return reapCurrentBatch("Test driven explicit reaping");
    }

    protected boolean reapCurrentBatch(String operatorName) {
        AgeBatch currentBatchRef = currentBatch.get();
        if (currentBatchRef.events.isEmpty()) {
            return true;
        }
        // We should not block here as the offer & reaper thread both does not block in any condition.
        if (batchReapingLock.tryLock()) {
            try {
                if (oldBatches.offer(currentBatchRef)) {
                    currentBatch.getAndSet(createNewBatch(null));
                    queueSizeCounter.incrementAndGet();
                    LOGGER.debug(String.format(
                            "[Reaping source: %s , Batching strategy: %s ] Reaped the old batch with size %s for subscriber: %s",
                            operatorName, batchingStrategy, currentBatchRef.events.size(), subscriberName));
                    oldBatchesQueueFull.set(false);
                    return true;
                } else {
                    oldBatchesQueueFull.set(true);
                    LOGGER.info(String.format(
                            "[Reaping source: %s , Batching strategy: %s ] Old batches queue for subscriber %s is full. Not reaping the batch till we get space.",
                            operatorName, batchingStrategy, subscriberName));
                }
            } finally {
                batchReapingLock.unlock();
            }
        } else {
            LOGGER.debug(String.format(
                    "[Reaping source: %s , Batching strategy: %s ] Subscriber: %s did not reap as there is another thread already reaping.",
                    operatorName, batchingStrategy, subscriberName));
        }
        return false;
    }

    protected AgeBatch createNewBatch(@Nullable SubscriberConfigProvider.SubscriberConfig subscribe) {
        return new AgeBatch();
    }

    /**
     * @author Nitesh Kant (nkant@netflix.com)
     */
    protected class AgeBatch implements EventBatch {

        @VisibleForTesting
        ConcurrentLinkedQueue events;

        protected AgeBatch() {
            events = new ConcurrentLinkedQueue();
        }

        @SuppressWarnings("unchecked")
        protected boolean addEvent(Object event) {
            return events.add(event);
        }

        @Override
        public Iterator iterator() {
            return events.iterator(); // This will happen only after we enqueue this batch to the oldBatches queue.
                                      // So, no mutations will happen to this events list after that and hence we can
                                      // not loose events that are added here but not reflecting in the iterator.
        }

        protected void clear() {
            events.clear();
        }
    }

    private class ReaperTask extends TimerTask {
        @Override
        public void run() {
            try {
                reapCurrentBatch("Reaper");
            } catch (Throwable th) {
                LOGGER.error(String.format(
                        "Reaper thread for subscriber: %s threw an error while reaping. Eating exception.",
                        subscriberName), th);
            }
        }
    }
}
