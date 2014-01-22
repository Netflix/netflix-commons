package com.netflix.eventbus.impl;

import com.netflix.eventbus.impl.EventBusImpl;
import com.netflix.eventbus.spi.EventBus;

/**
 * A simple provider to avoid direct binding to an implementation.
 *
 * @author Nitesh Kant (nkant@netflix.com)
 * 
 */
public class EventBusProvider {

    private static final EventBus bus = new EventBusImpl();

    public static EventBus getEventBus() {
        return bus;
    }
}
