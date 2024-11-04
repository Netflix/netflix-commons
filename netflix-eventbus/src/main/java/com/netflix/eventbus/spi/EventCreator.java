package com.netflix.eventbus.spi;

import java.util.List;
import java.util.Set;

/**
 * A contract for delayed creation of event(s) objects for conditional event publishing.
 * <p>
 * The responsibility of storing enough context to create the event(s) later lies on the implementations. As one would
 * imagine, this creator will mostly be stateful and hence be instantiated afresh for every call.
 * <p>
 * The event(s) will be created iff there exists a listener that will process these event(s). However, due to the concurrent
 * nature of the event bus, this may not always be the case, as the listeners may come and go between the existence
 * check and actual invocation.
 *
 * <h2>Multiple events</h2> A single event creator can create multiple events at the same time. This is required to
 * batch multiple event types together in one conditional publish to aid optimization of multiple publish calls. The
 * event bus takes care of managing the mapping between the event type and listeners that showed interest when
 * {@link EventBus#publishIffNotDead(EventCreator, Class[])} was called for this creator.
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
public interface EventCreator {

    /**
     * A callback when the event bus determines that there is atleast one listener for any of the passed event types to
     * the corresponding {@link EventBus#publishIffNotDead(EventCreator, Class[])} call.
     * <p>
     * The event bus makes sure to establish a mapping between the event types and the live listeners before
     * calling this method and publishes the created events only to the associated live listeners (determined before
     * this callback)
     *
     * @param liveEventTypes A subset of event types passed to the corresponding {@link EventBus#publishIffNotDead(EventCreator, Class[])}
     *                       call which have atleast one listener to receive the event. Events created only for these
     *                       event types will be published by the event bus.
     *
     * @return Newly created events corresponding to the live event types. <code>null</code> if no events are created.
     */
    List<?> createEvent(Set<Class<?>> liveEventTypes);
}
