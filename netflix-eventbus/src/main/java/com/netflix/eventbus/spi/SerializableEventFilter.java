package com.netflix.eventbus.spi;


/**
 * An optional feature for a {@link EventFilter} which indicates that the event filter can be converted back and forth
 * from a string.
 *
 * Typically, if the filter is an expression of a language, as defined by {@link com.netflix.eventbus.filter.lang}, then
 * it should implement this interface. On the other hand, if the filter is represented as code, implementation of this
 * interface is not required.
 *
 * Any persistence of an {@link EventFilter} will require the filter to implement this interface, else it will not be
 * persisted. Since, the filters created in code will typically be registered by the subscribers themselves, they
 * typically will not need any persistence.
 *
 * One will notice that this filter only supports converting the object representation to a string and not back as the
 * conversion from arbitrary string is handled by {@link com.netflix.eventbus.filter.EventFilterCompiler}. Such, factory
 * kind of methods are deliberately not provided here so that the filter classes do not have to provide any parsing
 * logic.
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
public interface SerializableEventFilter extends EventFilter {

    /**
     * The language in which this filter is expressed.
     *
     * @return the language in which this filter is expressed.
     */
    FilterLanguage getFilterLanguage();

    /**
     * Serializes this filter into the filter string in the language specified by
     * {@link com.netflix.eventbus.spi.SerializableEventFilter#getFilterLanguage()}.
     *
     * @return String representation of the filter.
     */
    String serialize();
}
