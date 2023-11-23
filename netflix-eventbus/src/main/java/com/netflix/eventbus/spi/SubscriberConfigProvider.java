package com.netflix.eventbus.spi;

import javax.annotation.Nullable;

/**
 * A contract to handle dynamic subscriber configuration. Since, any configuration provided by {@link Subscribe}
 * annotation is inherently compile-time &amp; constant, in cases where the configurations are to be taken from a property
 * file for instance, a subscriber must implement this interface and provide dynamic configurations.
 *
 * These configurations will be used once &amp; only once by eventbus at the time the subscriber is registered.
 *
 * All the subscriber methods in a class can be configured using this provider, by pinning a configuration to a key
 * which is attached to a subscriber method by the property {@link com.netflix.eventbus.spi.Subscribe#name()}.
 *
 * For a subscriber that implements interface, for any subscriber method, eventbus follows the following order to get
 * the configuration for that subscriber method:
 * <ul>
 <li>Calls {@link SubscriberConfigProvider#getConfigForName(String)} with the name as specified by
 {@link com.netflix.eventbus.spi.Subscribe#name()}</li>
 <li>If the configuration returned above is not <code>null</code> then uses this configuration for any field.</li>
 <li>If the configuration returned above is <code>null</code> then uses any configuration provided by the annotation
 as is.</li>
 </ul>
 *
 * @author Nitesh Kant
 */
public interface SubscriberConfigProvider {

    /**
     * Returns the configuration for the passed subscriber name.
     *
     * @param subscriberName Name of the subscriber.
     *
     * @return The configuration or <code>null</code> if the configuration for that subscriber is not supplied by this
     * provider.
     */
    @Nullable
    SubscriberConfig getConfigForName(String subscriberName);

    /**
     * Configuration for a subscriber. Any property that can be configured by {@link Subscribe} can also be configured
     * here.
     */
    interface SubscriberConfig {

        Subscribe.BatchingStrategy getBatchingStrategy();

        int getBatchAge();

        int getBatchSize();

        int getQueueSize();

        boolean syncIfAllowed();
    }
}
