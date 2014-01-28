package com.netflix.eventbus.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.eventbus.spi.EventBus;
import com.netflix.eventbus.spi.EventFilter;
import com.netflix.eventbus.spi.Subscribe;
import com.netflix.eventbus.spi.SubscriberConfigProvider;
import com.netflix.eventbus.spi.SyncSubscribersGatekeeper;
import com.netflix.eventbus.utils.EventBusUtils;
import com.netflix.servo.monitor.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.netflix.eventbus.utils.EventBusUtils.isAnEventBatch;

/**
 * An event consumer. An event consumer always consumes the events asynchronously and the events can be batched using
 * an appropriate {@link com.netflix.eventbus.spi.Subscribe.BatchingStrategy} <br/>
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
class EventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventConsumer.class);

    // we can try upto 5 times max in case the queue is full.
    private static final DynamicIntProperty maxRetriesOnQueueFull =
            DynamicPropertyFactory.getInstance().getIntProperty(EventBus.CONSUMER_QUEUE_FULL_RETRY_MAX_PROP_NAME,
                    EventBus.CONSUMER_QUEUE_FULL_RETRY_MAX_DEFAULT);

    private Class<?> targetEventClass;
    private final Method delegateSubscriber;
    private final Object subscriberClassInstance;
    private final CopyOnWriteArraySet<EventFilter> filters;

    private final EventBusImpl.ConsumerQueueSupplier.ConsumerQueue eventQueue;

    private final EventPoller poller;
    private final Thread pollerThread;
    private final Subscribe.BatchingStrategy batchingStrategy;

    private final EventConsumerStats stats;
    private final SubscriberConfigProvider.SubscriberConfig subscriberConfig;

    EventConsumer(Method subscriber, Object subscriberClassInstance, @Nullable EventFilter filter, Class<?> targetEventType,
                  EventBusImpl.ConsumerQueueSupplier queueSupplier) {
        Preconditions.checkArgument(subscriber.getDeclaringClass() == subscriberClassInstance.getClass(), "The subscriber method does not belong to the subscriber class.");

        this.delegateSubscriber = subscriber;
        this.subscriberClassInstance = subscriberClassInstance;
        targetEventClass = targetEventType;

        stats = new EventConsumerStats(
                subscriberClassInstance.getClass().getName() + "_" + delegateSubscriber.getName() + "_" +
                                       targetEventClass.getName(), EventBusImpl.STATS_COLLECTION_DURATION_MILLIS.get());
        subscriberConfig = EventBusUtils.getSubscriberConfig(subscriber, subscriberClassInstance);
        batchingStrategy = subscriberConfig.getBatchingStrategy();
        eventQueue = queueSupplier.get(delegateSubscriber, subscriberConfig, stats.QUEUE_SIZE_COUNTER);
        poller = new EventPoller();
        pollerThread = new Thread(poller);
        pollerThread.start();
        if (null != filter) {
            filters = new CopyOnWriteArraySet<EventFilter>(Arrays.asList(filter));
        } else {
            filters = new CopyOnWriteArraySet<EventFilter>();
        }
    }

    @SuppressWarnings("unchecked")
    void enqueue(Object event) {
        if (SyncSubscribersGatekeeper.isSyncSubscriber(subscriberConfig, event.getClass(), delegateSubscriber.getClass())) {
            LOGGER.debug(String.format("Sending a sync event to subscriber: %s. Set the property %s to false to disable sync consumption.",
                                       delegateSubscriber.toGenericString(), SyncSubscribersGatekeeper.ALLOW_SYNC_SUBSCRIBERS));
            processEvent(event);
            return;
        }

        Stopwatch start = stats.enqueueStats.start();
        try {
            int retries = 0;
            int maxRetries = maxRetriesOnQueueFull.get();
            while (!eventQueue.offer(event) && retries++ < maxRetries) {
                stats.QUEUE_OFFER_RETRY_COUNTER.increment();
                eventQueue.nonBlockingTake(); // removes and rejects.
                LOGGER.info(String.format("Subscriber: %s queue full, rejected one %s as a result of retries.",
                        delegateSubscriber.toGenericString(),
                        (Subscribe.BatchingStrategy.None == batchingStrategy) ? "event" : "batch"));
            }

            if (0 != retries) {
                LOGGER.info(String.format("Subscriber: %s %s one event after %s retries.",
                        delegateSubscriber.toGenericString(),
                        ((retries >= maxRetries) ? "rejected" : "accepted"),
                        (retries - 1)));
                if (retries >= maxRetries) {
                    stats.EVENT_ENQUEUE_REJECTED_COUNTER.increment();
                }
            }

        } finally {
            start.stop();
        }
    }

    void addFilters(EventFilter... filters) {
        this.filters.addAll(Arrays.asList(filters));
    }

    void removeFilters(EventFilter... filters) {
        this.filters.removeAll(Arrays.asList(filters));
    }

    void clearFilters() {
        this.filters.clear();
    }

    void shutdown() {
        poller.stop();
        eventQueue.clear();
        filters.clear();
    }

    Method getDelegateSubscriber() {
        return delegateSubscriber;
    }

    Object getContainerInstance() {
        return subscriberClassInstance;
    }

    Class<?> getTargetEventClass() {
        return targetEventClass;
    }

    Set<EventFilter> getAttachedFilters() {
        return filters;
    }

    @VisibleForTesting
    EventConsumerStats getStats() {
        return stats;
    }

    @VisibleForTesting
    SubscriberConfigProvider.SubscriberConfig getSubscriberConfig() {
        return subscriberConfig;
    }

    private void processEvent(Object event) {
        Stopwatch start = stats.consumptionStats.start();

        event = wrapIfBatched(event);

        if (applyFilters(event)) {
            try {
                delegateSubscriber.invoke(subscriberClassInstance, event);
            } catch (Exception e) {
                LOGGER.error("Failed to dispatch event: " + event + " to subscriber class: " +
                             subscriberClassInstance.getClass() + " and method: " + delegateSubscriber.toGenericString() +
                             ". Ignoring the event.", e);
            } finally {
                start.stop();
            }
        }
    }

    private boolean applyFilters(Object event) {
        if (isAnEventBatch(event)) { // For batches, the filters are run on demand i.e. in each next() call.
            return true;
        } else {
            return EventBusUtils.applyFilters(event, filters, stats.filterStats,
                                              "subscriber: " + delegateSubscriber.toGenericString(), LOGGER);
        }
    }

    private Object wrapIfBatched(Object event) {
        if (isAnEventBatch(event)) {
            return new BatchDecorator((EventBatch) event);
        }
        return event;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EventConsumer consumer = (EventConsumer) o;

        if (delegateSubscriber != null ? !delegateSubscriber.equals(consumer.delegateSubscriber)
                                       : consumer.delegateSubscriber != null) {
            return false;
        }
        if (filters != null ? !filters.equals(consumer.filters) : consumer.filters != null) {
            return false;
        }
        if (subscriberClassInstance != null ? !subscriberClassInstance.equals(consumer.subscriberClassInstance)
                                            : consumer.subscriberClassInstance != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = delegateSubscriber != null ? delegateSubscriber.hashCode() : 0;
        result = 31 * result + (subscriberClassInstance != null ? subscriberClassInstance.hashCode() : 0);
        result = 31 * result + (filters != null ? filters.hashCode() : 0);
        return result;
    }

    private class EventPoller implements Runnable {

        private volatile boolean stop;

        private void stop() {
            stop = true;
            pollerThread.interrupt();
        }

        @Override
        public void run() {
            LOGGER.info("Event consumer: " + delegateSubscriber.toGenericString() + " started.");
            while (!stop) {
                Object event;
                try {
                    event = eventQueue.blockingTake();
                    if (null != event) {
                        processEvent(event);
                    }
                } catch (InterruptedException e) {
                    LOGGER.info("Event consumer: " + delegateSubscriber.toGenericString() +
                                " interrupted. Can be the result of a stop call, if so, you will see a 'consumer stopped' log.");
                }
            }
            LOGGER.info("Event consumer: " + delegateSubscriber.toGenericString() + " stopped.");
        }
    }

    /**
     * A decorator for {@link com.netflix.eventbus.impl.EventBatch} to run filters when events are requested by the consumer. <br/>
     * The reason for this is that we can not run a filter on an event batch and running the filter on enqueue does it in
     * the publishing thread which is not desirable. The downside of this is that a consumer may get an empty batch.
     *
     * @author Nitesh Kant
     */
    class BatchDecorator implements Iterable {

        private final EventBatch batch;

        BatchDecorator(EventBatch batch) {
            this.batch = batch;
        }

        @Override
        public Iterator iterator() {
            return new BatchIterator(batch);
        }

        private class BatchIterator implements Iterator {

            private final PeekingIterator delegatePeekingIterator;

            @SuppressWarnings("unchecked")
            BatchIterator(EventBatch batch) {
                this.delegatePeekingIterator = Iterators.peekingIterator(batch.iterator());
                _ensureNextEventIsConsumable();
            }

            @Override
            public boolean hasNext() {
                return delegatePeekingIterator.hasNext();
            }

            @Override
            public Object next() {
                Object toReturn = delegatePeekingIterator.next(); // Since we call _fetchNext before hand, next() event is always valid.
                _ensureNextEventIsConsumable();
                return toReturn;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Event batch iterator does not support remove.");
            }

            private void _ensureNextEventIsConsumable() {
                if (delegatePeekingIterator.hasNext()) {
                    Object nextEvent = delegatePeekingIterator.peek();
                    if (!EventBusUtils.applyFilters(nextEvent, filters, stats.filterStats,
                                                    "subscriber: " + delegateSubscriber.toGenericString(), LOGGER)) {
                        // If next event is not consumable i.e. filtered, remove and see next.
                        delegatePeekingIterator.next();
                        delegatePeekingIterator.remove();
                        _ensureNextEventIsConsumable();
                    }
                }
            }
        }
    }
}
