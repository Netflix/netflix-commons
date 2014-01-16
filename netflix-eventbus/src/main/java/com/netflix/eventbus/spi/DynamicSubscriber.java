package com.netflix.eventbus.spi;

/**
 * Eventbus subscribers, by design, are statically tied to a particular type of object i.e. the class of the event it
 * is interested in. This in most cases is beneficial and easy to use, however, in some cases (typically event agnostic,
 * middlemen, which stores the event for later investigation), the event processing is much the same irrespective of the
 * type of event it receives. In such a case, it will be an overhead (even if possible) to define a subscriber for
 * each kind of event existing in the system. <br/>
 * This special subscriber is a way to achieve dynamic interests (immutable after registration) in subscribers. Although,
 * this subscriber can only listen to one type of event but the type is defined at runtime as opposed to compile time
 * in a normal subscriber.<br/>
 * Although, this subscriber does not mandate that the subscriber must have a subscribe method (one annotated with
 * {@link Subscribe}) listening to event type {@link Object} but in most of the cases, such a subscribe method will
 * prove to be more useful. {@link EventBus} allows specifying the subscribe method with argument {@link Object}
 * <em>only</em> for these subscribers. <br/>
 *
 * <b>Implementations must only have a single subscriber method or else the registration with eventbus will be rejected.</b>
 * This limitation is imposed as we do not want to make runtime decisions on which subscribe method an event must be
 * directed to.
 *
 * @author Nitesh Kant
 */
public interface DynamicSubscriber {

    /**
     * Returns the event type i.e. the class of the event that this subscriber will be interested in. <br/>
     * {@link EventBus} allows specifying the subscribe method with argument {@link Object} <em>only</em> for
     * these subscribers.<br/>
     * The implementers of this interface must make sure that the subscribe method accepts the event class returned by
     * this method.
     *
     * @return The event type i.e. the class of the event that this subscriber will be interested in. <br/>
     */
    Class<?> getEventType();
}
