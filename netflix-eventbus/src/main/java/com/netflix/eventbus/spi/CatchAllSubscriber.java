package com.netflix.eventbus.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A special subscriber that receives all events published to {@link EventBus}. This is always registered with eventbus
 * but is disabled. In order to enable this subscriber call
 * {@link com.netflix.eventbus.spi.EventBus#enableCatchAllSubscriber(java.util.concurrent.BlockingQueue)}
 * with the sink (receiver) for all events.
 *
 * This subscriber will only call {@link java.util.concurrent.BlockingQueue#offer(Object)} on this sink to avoid any sort
 * of blocking. Any events for which the offer call fails are rejected.
 *
 * As any other {@link EventBus} subscriber, this subscriber is async i.e. any events received by this subscriber are
 * pushed by the eventbus in async mode. The queue size for this is {@link CatchAllSubscriber#SUBSCRIBER_QUEUE_SIZE} and
 * there is no batching done for events to reduce memory overheads.
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
public final class CatchAllSubscriber {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatchAllSubscriber.class);

    public static final int SUBSCRIBER_QUEUE_SIZE = 100;

    private AtomicReference<BlockingQueue> sink = new AtomicReference<BlockingQueue>(null);

    @Subscribe(queueSize = SUBSCRIBER_QUEUE_SIZE)
    @SuppressWarnings({"unchecked", "unused"})
    public void receive(Object event) {
        BlockingQueue sinkNow = this.sink.get();
        if (null != sinkNow && !sinkNow.offer(event)) {
            LOGGER.info("CatchAllSubscriber sink full, rejected an event.");
        }
    }

    public boolean enable(BlockingQueue sink) {
        return this.sink.compareAndSet(null, sink);
    }

    public void disable() {
        this.sink.set(null);
    }

    public boolean isEnabled() {
        return this.sink.get() != null;
    }
}
