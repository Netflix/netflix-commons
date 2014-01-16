package com.netflix.eventbus.filter;

import javax.annotation.Nullable;

import com.netflix.eventbus.spi.EventFilter;

public class AlwaysFalseEventFilter implements EventFilter {
    public static final AlwaysFalseEventFilter INSTANCE = new AlwaysFalseEventFilter();
    
    // There's no point of creating multiple instance of this class
    private AlwaysFalseEventFilter() {
    }
    
    
    @Override
    public boolean apply(@Nullable Object input) {
        return false;
    }

    @Override
    public String getLanguage() {
        return "constant";
    }

    @Override
    public String serialize() {
        return "false";
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AlwaysFalseEventFilter []");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Boolean.FALSE.hashCode();
    }

    @Override
    public boolean equals(Object obj){
        return obj instanceof AlwaysFalseEventFilter;
    }
}
