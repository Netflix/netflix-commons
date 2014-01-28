package com.netflix.eventbus.filter.lang;

import com.netflix.eventbus.spi.EventFilter;

/**
 * General contract for any filter language which relates to a methodology of converting a language expression to a
 * valid {@link com.netflix.eventbus.spi.EventFilter} instance consumable by {@link com.netflix.eventbus.spi.EventBus}
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
public interface FilterLanguageSupport<T> {

    /**
     * Converts the passed filter object to a valid {@link EventFilter}.
     *
     * @param filter Filter object to convert.
     *
     * @return {@link EventFilter} corresponding to the passed filter.
     *
     * @throws InvalidFilterException If the passed filter was invalid.
     */
    public EventFilter convert(T filter) throws InvalidFilterException;
}
