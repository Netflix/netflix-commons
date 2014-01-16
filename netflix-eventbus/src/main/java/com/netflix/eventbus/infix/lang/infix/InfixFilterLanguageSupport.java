package com.netflix.eventbus.infix.lang.infix;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Predicate;
import com.netflix.eventbus.infix.lang.FilterLanguageSupport;
import com.netflix.eventbus.infix.lang.InvalidFilterException;
import com.netflix.eventbus.spi.EventFilter;
import com.netflix.infix.InfixCompiler;

/**
 * @author Nitesh Kant (nkant@netflix.com)
 */
public class InfixFilterLanguageSupport implements FilterLanguageSupport<String> {

    @Override
    public EventFilter convert(String filter) throws InvalidFilterException {
        try {
            final Predicate<Object> predicate = new InfixCompiler().compile(filter);
            return new InfixEventFilter(predicate, filter);
        } catch (Exception e) {
            throw new InvalidFilterException("Error compiling filter : " + StringUtils.abbreviate(filter, 100), e, null);
        }
    }
}
