package com.netflix.eventbus.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Consumers for {@link EventBus} must annotate their consumer methods with this annotation. <br/>
 * A consumer is asynchronous, however it can choose to batch events consumed at a time using a batching strategy. <br/>
 * A consumer can indicate if it <b>favors</b> synchronous event consumption, provided, it is allowed in the current
 * environment, by setting the property {@link SyncSubscribersGatekeeper#ALLOW_SYNC_SUBSCRIBERS} to true.<br/>
 * A consumer is always assumed to be thread-safe. <br/>
 * Any method annotated with this must have one and only one argument which is the event object that it is supposed to
 * handle. <br/>
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {

    public static final String NO_NAME = "none";

    /**
     * Batching strategy for event batches.
     */
    enum BatchingStrategy {
        /**
         *  The events will be batched based on the time elapsed between the addition of the first entry and now.
         * The time is always in milliseconds and is defined by the property {@link com.netflix.eventbus.spi.Subscribe#batchAge}
         **/
        Age,

        /**
         * The events will be dispatched if the number of events in the batch exceeds the threshold as defined by
         * {@link com.netflix.eventbus.spi.Subscribe#batchSize} or the age as defined by
         * {@link Subscribe#batchAge}.
         */
        SizeOrAge,

        /**
         * No batching, the events will be dispatched one at a time.
         */
        None
    }

    /**
     * A name for this subscriber, this is only required if you want to have a dynamic configuration via
     * {@link SubscriberConfigProvider} AND each subscriber method in the class has a different configuration.
     *
     * @return A name, unique in a single subscriber class. This name does <b>not</b> need to be unique amongst all
     * subscriber classes.
     */
    String name() default NO_NAME;

    /**
     * Returns the batching strategy for this subscriber. If a subscriber chooses a batching strategy other than
     * {@link BatchingStrategy#None}, the argument to the subscriber method <b>must</b> be an {@link Iterable}.
     * <b>Event batches does not support removal.</b>
     *
     * @return The batching strategy for this subscriber.
     */
    BatchingStrategy batchingStrategy() default BatchingStrategy.None;

    /**
     * The threshold for the age of the batch in milliseconds since the first entry was added. Only considered if the
     * batching strategy is {@link BatchingStrategy#Age} or {@link BatchingStrategy#SizeOrAge}
     *
     * @return The age of the batch in milliseconds.
     */
    int batchAge() default 0;

    /**
     * The threshold for the size of the batch. Only considered if the batching strategy is {@link BatchingStrategy#SizeOrAge}
     *
     * @return The threshold for the size of the batch.
     */
    int batchSize() default 1;

    /**
     * The queue size for the consumer. In case, the consumer receives batches of events, this will be the number of
     * batches and <b>not</b> individual events.
     *
     * @return The queue size for the consumer.
     */
    int queueSize() default -1;

    /**
     * A backdoor in the eventbus to allow synchronous events consumption. This mode is controlled by the property
     * {@link SyncSubscribersGatekeeper#ALLOW_SYNC_SUBSCRIBERS}, which when set to true, allows registration of synchronous consumers. <br/>
     * <b>Setting this property to true, does NOT guarantee synchronous event consumption. It is only available when
     * the property {@link SyncSubscribersGatekeeper#ALLOW_SYNC_SUBSCRIBERS} is set to true at the time of event publishing.</b>
     * If the property is set to <code>false</code> the events will be sent to the consumer asynchronously.
     *
     * @return <code>true</code> if this subscriber favors synchronous consumption of events. <code>false</code>  by
     * default.
     */
    boolean syncIfAllowed() default false;
}
