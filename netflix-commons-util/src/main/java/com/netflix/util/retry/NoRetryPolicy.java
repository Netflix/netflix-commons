package com.netflix.util.retry;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Retry policy that implements no retry at all.
 * 
 * @author elandau
 */
public class NoRetryPolicy implements RetryPolicy {
    
    @Override
    public <R> ListenableFuture<R> submit(Callable<R> callable, ScheduledExecutorService executor) {
        SettableFuture<R> future = SettableFuture.create();
        try {
            callable.call();
        }
        catch (Throwable t) {
            future.setException(t);
        }
        return future;
    }
}
