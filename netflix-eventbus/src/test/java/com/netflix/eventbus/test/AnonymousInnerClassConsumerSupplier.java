package com.netflix.eventbus.test;

import com.netflix.eventbus.impl.NFEventBusTest;
import com.netflix.eventbus.spi.Subscribe;

/**
 * @author Nitesh Kant
 */
public class AnonymousInnerClassConsumerSupplier {

    /**
     * This is just a way to provide a anonymous class with non-package private access modifier.
     *
     * @param event The type of event, consumer should consume.
     * @param <T> Type of the event
     *
     * @return Consumer.
     */
    public static <T extends NFEventBusTest.Event> Object getAnonymousInnerClassConsumer(Class<T> event) {
        return new Object() {

            @Subscribe
            protected void consume(T event) {
                // Helleluja!!!
            }
        };
    }
}
