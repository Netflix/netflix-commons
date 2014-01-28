package com.netflix.eventbus.filter.lang.infix;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;

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
    
    @Override
    public int hashCode() {
        return original != null ? original.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("InfixEventFilter");
        sb.append("{input=").append(StringUtils.abbreviate(original, 256));
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InfixEventFilter that = (InfixEventFilter) o;

        if (predicate != null ? !predicate.equals(that.predicate) : that.predicate != null) {
            return false;
        }

        return true;
    }

}
