package com.netflix.util.retry;

import java.util.concurrent.Callable;

/**
 * Abstraction for a retry policy around a Callable.  
 * 
 * @author elandau
 */
public interface RetryPolicy {
    /**
     * Wrap a Callable with retry logic.  Calling call() on the returned Callable
     * will execute the original Callable with retries.  The retry policy will
     * attempt to retry on any exception, except for NotRetryableException and
     * InterruptedException which should be thrown by the original callback to 
     * force exiting the retry loop.
     * 
     * @param callable
     * @return
     */
    public <R> Callable<R> wrap(Callable<R> callable);
}
