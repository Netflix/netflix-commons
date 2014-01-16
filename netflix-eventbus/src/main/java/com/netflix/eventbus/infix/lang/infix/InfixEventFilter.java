package com.netflix.eventbus.infix.lang.infix;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.netflix.eventbus.spi.EventFilter;

public class InfixEventFilter implements EventFilter {

    private final Predicate<Object> predicate;
    
    public InfixEventFilter(Predicate<Object> predicate) {
        this.predicate = predicate;
    }
    
    @Override
    public boolean apply(@Nullable Object input) {
        return predicate.apply(input);
    }

}
