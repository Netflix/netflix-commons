package com.netflix.util.retry;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Retry context for an operation
 * 
 * @author elandau
 */
public interface RetryContext {
    /**
     * Execute the callable in the caller thread's context but schedule retries
     * on the provided executor.
     * 
     * @param callable
     * @param executor
     * @return
     */
    public <R> Future<R> call(Callable<R> callable, ScheduledExecutorService executor);
    
    /**
     * Return the number of attempts, or number of times +1 nextBackoffDelay was called
     * @return
     */
    public long getAttemptCount();
    
    /**
     * Return elapsed time since the context was created
     * @param units
     * @return
     */
    public long getEllapsedTime(TimeUnit units);
}
