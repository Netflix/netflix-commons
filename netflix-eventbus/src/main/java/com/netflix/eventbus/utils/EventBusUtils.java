package com.netflix.eventbus.utils;

import com.google.common.base.Preconditions;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.eventbus.impl.EventBatch;
import com.netflix.eventbus.spi.DynamicSubscriber;
import com.netflix.eventbus.spi.EventBus;
import com.netflix.eventbus.spi.EventFilter;
import com.netflix.eventbus.spi.Subscribe;
import com.netflix.eventbus.spi.SubscriberConfigProvider;
import com.netflix.eventbus.spi.SubscriberInfo;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.StatsTimer;
import com.netflix.servo.monitor.Stopwatch;
import com.netflix.servo.stats.StatsConfig;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * General utility methods for {@link com.netflix.eventbus.spi.EventBus}
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
public class EventBusUtils {

    private static final DynamicIntProperty queueSizeDefault =
            DynamicPropertyFactory.getInstance().getIntProperty(EventBus.CONSUMER_QUEUE_SIZE_DEFAULT_PROP_NAME,
                    EventBus.CONSUMER_QUEUE_SIZE_DEFAULT);

    /**
     * Returns an appropriate consumer queue size for the passed <code>subscribe</code> annotation. This method defaults
     * the size to the one specified in the fast property {@link EventBus#CONSUMER_QUEUE_SIZE_DEFAULT_PROP_NAME}
     *
     *
     * @param subscribe The annotation for which the queue size is to be retrieved.
     *
     * @return The queue size.
     */
    public static int getQueueSize(SubscriberConfigProvider.SubscriberConfig subscribe) {
        int queueSize = subscribe.getQueueSize();
        if(queueSize <= 0) {
            queueSize = queueSizeDefault.get();
        }
        return queueSize;
    }

    /**
     * Returns configuration for the passed subscriber method. This configuration can be obtained from the
     * {@link Subscribe} annotation on the method or from {@link SubscriberConfigProvider} if the subscriber implements
     * that interface.
     *
     * @param subscriber The instance of the subscriber that contains the subscriber method.
     * @param  subMethod Method for which the configuration has to be found.
     *
     * @return Subscriber configuration.
     */
    public static SubscriberConfigProvider.SubscriberConfig getSubscriberConfig(Method subMethod, Object subscriber) {
        Preconditions.checkNotNull(subscriber);
        Preconditions.checkNotNull(subMethod);

        Subscribe annotation = subMethod.getAnnotation(Subscribe.class);
        if (null == annotation) {
            throw new IllegalArgumentException(String.format("Subscriber method %s does not contain a subscriber annotation.", subMethod.toGenericString()));
        }
        SubscriberConfigProvider.SubscriberConfig config = null;
        if (SubscriberConfigProvider.class.isAssignableFrom(subscriber.getClass())) {
            config = ((SubscriberConfigProvider) subscriber).getConfigForName(annotation.name());
        }

        if (null == config) {
            config = new AnnotationBasedSubscriberConfig(annotation);
        }

        return config;
    }

    /**
     * Same as calling {@link #getSubscriberConfig(java.lang.reflect.Method, Object)} with
     * {@link com.netflix.eventbus.spi.SubscriberInfo#getSubscriberMethod()} and
     * {@link com.netflix.eventbus.spi.SubscriberInfo#getSubscriberInstance()}
     *
     * @param subscriberInfo The instance of the subscriber that contains the subscriber method.
     *
     * @return Subscriber configuration.
     */
    public static SubscriberConfigProvider.SubscriberConfig getSubscriberConfig(SubscriberInfo subscriberInfo) {
        return getSubscriberConfig(subscriberInfo.getSubscriberMethod(), subscriberInfo.getSubscriberInstance());
    }

    /**
     * Deduce whether the passed event is an event batch.
     *
     * @param event The event to inspect.
     *
     * @return <code>true</code> if the event is a batch.
     */
    public static boolean isAnEventBatch(Object event) {
        return EventBatch.class.isAssignableFrom(event.getClass());
    }

    /**
     * Returns the event class the passed subscriber is interested in. This will generally be the argument of the
     * subscriber method, except when the subscriber is a {@link DynamicSubscriber}, in which case the event type will
     * be as returned by {@link com.netflix.eventbus.spi.DynamicSubscriber#getEventType()}.
     * <b>It is important that the subscriber method is valid as evaluated by
     * {@link com.netflix.eventbus.impl.SubscriberValidator}</b>
     *
     * @param subscriber The subscriber instance in question.
     * @param subMethod The subscriber method is question.
     *
     * @return The event class this subscriber is interested in.
     */
    public static Class<?> getInterestedEventType(Object subscriber, Method subMethod) {
        Class<?> interestedEventType = (DynamicSubscriber.class.isAssignableFrom(subscriber.getClass()))
                          ? ((DynamicSubscriber) subscriber).getEventType()
                          : subMethod.getParameterTypes()[0];/* The subscriber method must be valid here. */
        Subscribe annotation = subMethod.getAnnotation(Subscribe.class);
        if (annotation.batchingStrategy() != Subscribe.BatchingStrategy.None
            && Iterable.class.isAssignableFrom(interestedEventType)) {
            // Batch consumer, the parameter type of Iterable is the actual event type.
            Type[] genericMethodParams = subMethod.getGenericParameterTypes();
            ParameterizedType interestedEventParam = (ParameterizedType) genericMethodParams[0]; // Validation ensures that the argument is generic Iterable.
            Type[] iterableTypeParams = interestedEventParam.getActualTypeArguments();
            // Now we have the generic parameter for the Iterable, which essentially is always 1.
            if (iterableTypeParams[0] instanceof ParameterizedType) {
                // This is the case where the Iterable paramter itself is a generic, eg:
                // Iterable<List<String>>
                // in such a case, the iterableTypeParams will not be the class of the interested event type.
                return (Class<?>) ((ParameterizedType)iterableTypeParams[0]).getRawType();
            } else {
                // The iterableTypeParams[0] itself is the class of the actual event.
                return (Class<?>) iterableTypeParams[0];
            }
        } else {
            return interestedEventType;
        }
    }

    /**
     * Utility method to apply filters for an event, this can be used both by publisher & subscriber code.
     *
     * @param event The event to apply filter on.
     * @param filters Filters to apply.
     * @param filterStats Stats timer for applying the filter.
     * @param invokerDesc A string description for the invoker, this is required just for logging.
     * @param logger Logger instance to use for logging.
     *
     * @return <code>true</code> if the event should be processed, <code>false</code> if the event should not be
     * processed any further i.e. it is filtered out. This will log a debug message when the event is filtered.
     */
    public static boolean applyFilters(Object event, Set<EventFilter> filters, StatsTimer filterStats,
                                       String invokerDesc, Logger logger) {
        if (filters.isEmpty()) {
            return true;
        }
        Stopwatch filterStart = filterStats.start();
        try {
            for (EventFilter filter : filters) {
                if (!filter.apply(event)) {
                    logger.debug(
                            "Event: " + event + " filtered out for : " + invokerDesc + " due to the filter: " + filter);
                    return false;
                }
            }
            return true;
        } finally {
            filterStart.stop();
        }
    }

    public static StatsTimer newStatsTimer(String monitorName, long collectionDurationInMillis) {
        MonitorConfig.Builder monitorConfigBuilder = MonitorConfig.builder(monitorName);
        StatsConfig.Builder statsConfigBuilder = new StatsConfig.Builder();
        statsConfigBuilder.withComputeFrequencyMillis(collectionDurationInMillis);
        statsConfigBuilder.withPublishMean(true);
        statsConfigBuilder.withPublishMin(true);
        statsConfigBuilder.withPublishMax(true);
        statsConfigBuilder.withPublishStdDev(true);
        statsConfigBuilder.withPublishVariance(true);
        return new StatsTimer(monitorConfigBuilder.build(), statsConfigBuilder.build());
    }

    private static class AnnotationBasedSubscriberConfig implements SubscriberConfigProvider.SubscriberConfig {

        private final Subscribe annotation;

        public AnnotationBasedSubscriberConfig(Subscribe annotation) {
            this.annotation = annotation;
        }

        @Override
        public Subscribe.BatchingStrategy getBatchingStrategy() {
            return annotation.batchingStrategy();
        }

        @Override
        public int getBatchAge() {
            return annotation.batchAge();
        }

        @Override
        public int getBatchSize() {
            return annotation.batchSize();
        }

        @Override
        public int getQueueSize() {
            return annotation.queueSize();
        }

        @Override
        public boolean syncIfAllowed() {
            return annotation.syncIfAllowed();
        }
    }
}
