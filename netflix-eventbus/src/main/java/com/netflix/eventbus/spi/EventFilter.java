package com.netflix.eventbus.spi;

import com.google.common.base.Predicate;

/**
 * Top level filter used by the event bus.  A filter may be created in code
 * or defined using a scripting language.  
 * 
 * @author elandau
 *
 */
public interface EventFilter extends Predicate<Object> {
    /**
     * @return String describing the underlying filter language
     */
    public String getLanguage();
    
    /**
     * @return String representing the complete filter definition.  May be null if
     *         the filter is written in code.
     */
    public String serialize();
}   
