package com.netflix.jersey.guice.providers.exception;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Interface for implementing a custom mapper.  
 * 
 * @see ThrowableExceptionMapper
 * @author elandau
 *
 */
public interface CustomThrowableExceptionMapper extends ExceptionMapper<Throwable> {
    /**
     * @param exception The exception
     * @param request   Request context that can be used to determine the context and response format
     * @return  Return true if this mapper should be used to map the exception
     */
    public boolean canMap(Throwable exception, HttpServletRequest request);
}
