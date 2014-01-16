package com.netflix.eventbus.impl;

import com.netflix.eventbus.spi.CatchAllSubscriber;
import com.netflix.eventbus.spi.DynamicSubscriber;
import com.netflix.eventbus.spi.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A validator for subscribers to ensure that they adhere to our rules. <p/>
 * The rules specified at {@link com.netflix.eventbus.spi.InvalidSubscriberException} are the cases that this class
 * validates.
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
class SubscriberValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberValidator.class);

    /**
     * Validates and returns a map of errors per offending method, if any.
     *
     * @param subscriber Subscriber to validate.
     *
     * @return Map of method definition against error. Empty map if none found.
     */
    @SuppressWarnings("fallthrough")
    static Map<Method, String> validate(Object subscriber, List<Method> subscriberMethods) {
        Map<Method, String> errors = new HashMap<Method, String>(subscriberMethods.size());
        for (Method method : subscriberMethods) {
            Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
            if (null != subscribeAnnotation) {
                issueWarningsIfPresent(subscribeAnnotation, subscriber, method);
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) {
                    errors.put(method, String.format(
                            "Subscriber: %s's Method: %s is annotated as a subscriber but defines more that one arguments.",
                            subscriber.getClass(), method.toGenericString()));
                } else if (parameterTypes[0].equals(Object.class)
                           && !subscriber.getClass().equals(CatchAllSubscriber.class)
                           && !DynamicSubscriber.class.isAssignableFrom(subscriber.getClass())) {
                    errors.put(method, String.format(
                            "Subscriber: %s's Method: %s is a subscriber for java.lang.Object, that is too broad an interest.",
                            subscriber.getClass(), method.toGenericString()));
                } else if (DynamicSubscriber.class.isAssignableFrom(subscriber.getClass())
                           && !parameterTypes[0].equals(Object.class)) {
                    Class<?> targetedEventType = ((DynamicSubscriber) subscriber).getEventType();
                    if (!parameterTypes[0].isAssignableFrom(targetedEventType)) {
                        errors.put(method, String.format(
                                "Dynamic subscriber: %s's Method: %s's argument is not compatible with the interested event type %s.",
                                subscriber.getClass(), method.toGenericString(), targetedEventType.getName()));
                    }
                } else if (subscribeAnnotation.batchingStrategy() != Subscribe.BatchingStrategy.None) {
                    if (!(Iterable.class.isAssignableFrom(parameterTypes[0]))) {
                        errors.put(method, String.format(
                                "Subscriber: %s's Method: %s is annotated with batching strategy: %s but does not accept an Iterable argument.",
                                subscriber.getClass(), method.toGenericString(),
                                subscribeAnnotation.batchingStrategy()));
                    } else {
                        Type[] genericParameterTypes = method.getGenericParameterTypes();
                        if (!(genericParameterTypes[0] instanceof ParameterizedType)) {
                            errors.put(method, String.format(
                                    "Subscriber: %s's Method: %s is a subscriber for java.lang.Object, that is too broad an interest.",
                                    subscriber.getClass(), method.toGenericString()));
                        }
                    }

                    switch (subscribeAnnotation.batchingStrategy()) {
                        case SizeOrAge:
                            if (subscribeAnnotation.batchSize() <= 1) {
                                errors.put(method, String.format(
                                        "Subscriber: %s's Method: %s is annotated with batching strategy: %s but does define a batch size.",
                                        subscriber.getClass(), method.toGenericString(),
                                        subscribeAnnotation.batchingStrategy()));
                            }
                        case Age:
                            if (subscribeAnnotation.batchAge() <= 0) {
                                errors.put(method, String.format(
                                        "Subscriber: %s's Method: %s is annotated with batching strategy: %s but does define a batch age.",
                                        subscriber.getClass(), method.toGenericString(),
                                        subscribeAnnotation.batchingStrategy()));
                            }
                            break;
                    }
                }
            }
        }
        return errors;
    }

    private static void issueWarningsIfPresent(Subscribe subscribeAnnotation, Object subscriber, Method method) {
        if (subscribeAnnotation.syncIfAllowed()) {
            if (subscribeAnnotation.batchingStrategy() != Subscribe.BatchingStrategy.None) {
                LOGGER.warn(String.format(
                        "Subscriber: %s's Method: %s is annotated with batching strategy: %s and favors synchronous event consumption."
                        +
                        " Synchronous event consumption does not allow batching. This configuration will be honored if synchronous event"
                        +
                        "consumption is not allowed.",
                        subscriber.getClass(), method.toGenericString(),
                        subscribeAnnotation.batchingStrategy()));
            }
            if (subscribeAnnotation.queueSize() != -1) {
                LOGGER.warn(String.format(
                        "Subscriber: %s's Method: %s is annotated with a queue size: %s and favors synchronous event consumption."
                        +
                        " Synchronous event consumption does not allow queuing. This configuration will be honored if synchronous event"
                        +
                        "consumption is not allowed.",
                        subscriber.getClass(), method.toGenericString(),
                        subscribeAnnotation.queueSize()));
            }
        }
    }
}
