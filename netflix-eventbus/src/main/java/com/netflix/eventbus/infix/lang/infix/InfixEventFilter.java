package com.netflix.eventbus.infix.lang.infix;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.netflix.eventbus.spi.EventFilter;

public class InfixEventFilter implements EventFilter {
    public static final String INFIX_LANGUAGE_NAME = "infix";
    
    private final Predicate<Object> predicate;
    private final String original;
    
    public InfixEventFilter(Predicate<Object> predicate, String original) {
        this.predicate = predicate;
        this.original = original;
    }
    
    public InfixEventFilter(Predicate<Object> predicate) {
        this.predicate = predicate;
        this.original = null;
    }
    
    @Override
    public boolean apply(@Nullable Object input) {
        return predicate.apply(input);
    }

    @Override
    public String getLanguage() {
        return INFIX_LANGUAGE_NAME;
    }

    @Override
    public String serialize() {
        return original;
    }
}
