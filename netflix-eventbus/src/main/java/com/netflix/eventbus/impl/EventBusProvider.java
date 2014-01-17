package com.netflix.eventbus.impl;

import com.netflix.eventbus.impl.EventBusImpl;
import com.netflix.eventbus.spi.EventBus;

/**
 * A simple provider to avoid direct binding to an implementation.
 *
 * @author Nitesh Kant (nkant@netflix.com)
 * 
 * @deprecated Either create an instance of EventBusImpl directly or use a DI library to 
 * inject it as a singleton
 */
@Deprecated
public class EventBusProvider {

    private static final EventBus bus = new EventBusImpl();

    public static EventBus getEventBus() {
        return bus;
    }
}
