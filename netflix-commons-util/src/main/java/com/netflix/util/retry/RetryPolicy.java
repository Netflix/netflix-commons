package com.netflix.util.retry;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Abstraction for a RetryPolicy that determines the number of retries and backoff
 * amount based on the number of retry attempts and elapsed time since an 
 * operation was started.  Note that a concrete RetryPolicy is stateless and receives
 * the retry state in the form of an attempt count and elapsedMillis from the caller.
 * 
 * @author elandau
 */
public interface RetryPolicy {
    /**
     * Execute the callable in the caller thread's contxt but enqueue any retries
     * to the provided executor.
     * 
     * @param callable
     * @param executor
     * @return
     */
    public <R> ListenableFuture<R> submit(Callable<R> callable, ScheduledExecutorService executor);
}
