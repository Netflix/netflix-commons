package com.netflix.eventbus.spi;

import java.lang.reflect.Method;

/**
 * Metadata about a subscriber. This does <b>not</b> define any characterstics of the subscriber, its just a metadata
 * about any subscriber. <br/>
 * This class is designed to be immutable.
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
public class SubscriberInfo {

    private final Method subMethod;
    private final Object subInstance;

    public SubscriberInfo(Method subMethod, Object subInstance) {
        this.subMethod = subMethod;
        this.subInstance = subInstance;
    }

    /**
     * Returns the method in the subscriber class that is subscribing to a particular event.
     *
     * @return The method in the subscriber class that is subscribing to a particular event.
     */
    public Method getSubscriberMethod() {
        return subMethod;
    }

    /**
     * Returns the instance of the class that this subscriber method belongs.
     *
     * @return The instance of the class that this subscriber method belongs.
     */
    public Object getSubscriberInstance() {
        return subInstance;
    }
}
