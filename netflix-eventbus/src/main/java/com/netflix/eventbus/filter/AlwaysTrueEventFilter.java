package com.netflix.eventbus.filter;

import javax.annotation.Nullable;

import com.netflix.eventbus.spi.EventFilter;

public class AlwaysTrueEventFilter implements EventFilter {
    public static final AlwaysTrueEventFilter INSTANCE = new AlwaysTrueEventFilter();
    
    // There's no point of creating multiple instance of this class
    private AlwaysTrueEventFilter() {
    }
    
    @Override
    public boolean apply(@Nullable Object input) {
        return true;
    }

    @Override
    public String getLanguage() {
        return "Constant";
    }

    @Override
    public String serialize() {
        return "true";
    }

    @Override
    public String toString() {
        return "AlwaysTrueEventFilter []";
    }

    @Override
    public int hashCode() {
        return Boolean.TRUE.hashCode();
    }

    @Override
    public boolean equals(Object obj){
        return obj instanceof AlwaysTrueEventFilter;
    }
}
