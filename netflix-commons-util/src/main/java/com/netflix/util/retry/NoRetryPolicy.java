package com.netflix.util.retry;

import java.util.concurrent.Callable;

/**
 * Retry policy that implements no retry at all.
 * 
 * @author elandau
 */
public class NoRetryPolicy implements RetryPolicy {
    @Override
    public <R> Callable<R> wrap(final Callable<R> callable) {
        return new Callable<R>() {
            @Override
            public R call() throws Exception {
                return callable.call();
            }
        };
    }
}
