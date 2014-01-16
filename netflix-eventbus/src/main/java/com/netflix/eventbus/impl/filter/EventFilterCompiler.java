package com.netflix.eventbus.impl.filter;

import com.netflix.eventbus.filter.lang.FilterLanguageSupport;
import com.netflix.eventbus.filter.lang.InvalidFilterException;
import com.netflix.eventbus.filter.lang.infix.InfixFilterLanguageSupport;

/**
 * A compiler to compile the event filter from a language specified in {@link com.netflix.eventbus.filter.lang} to an
 * {@link com.netflix.eventbus.spi.EventFilter} for consumption by {@link com.netflix.eventbus.spi.EventBus}
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
public class EventFilterCompiler {

    private static FilterLanguageSupport<String> infixSupport = new InfixFilterLanguageSupport();

    /**
     * Compiles a filter expressed in infix notation to an {@link EventFilter} instance.
     *
     * @param filter Filter to compile.
     *
     * @return {@link EventFilter} instance compiled from the passed filter.
     *
     * @throws InvalidFilterException If the input filter is invalid.
     */
//    public static EventFilter compileInfixNotation(String filter) throws InvalidFilterException {
//        return infixSupport.convert(filter);
//    }
}
