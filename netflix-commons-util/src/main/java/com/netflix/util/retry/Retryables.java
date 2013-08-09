package com.netflix.util.retry;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ListenableFuture;

public class Retryables {
    public static <R> ListenableFuture<R> execute(final Callable<R> callable, final RetryPolicy policy, final ScheduledExecutorService executor) {
        
        final WrappedSettableFuture<R> future = WrappedSettableFuture.create();
        Future<Void> internalFuture = executor.submit(new Callable<Void>() {
            /**
             * Keep track of the number attempts since the first submit to the executor
             */
            int       attemptCount = 0;                         
            
            /**
             * Keep track of time since the callable is first created
             */
            Stopwatch sw           = new Stopwatch().start();
            
            /**
             * Wrap the callable and set the future's value or exception based on success or failure
             * after the max number of retry attempts
             */
            @Override
            public Void call() throws Exception {
                try {
                    if (!future.isCancelled()) {
                        R result = callable.call();
                        future.set(result);
                    }
                }
                catch (InterruptedException e) {
                    // Always exit if interrupted
                    future.setException(e);
                }
                catch (Exception e) {
                    // If exception is not retry-able then don't event try the retry logic 
                    if (e instanceof NotRetryableException)
                        future.setException(e);
                    
                    try {
                        ++attemptCount;
                        long delay = policy.nextBackoffDelay(attemptCount, sw.elapsed(TimeUnit.MILLISECONDS));
                        // Max retries reached
                        if (delay == -1)
                            future.setException(e);
                        // No delay
                        else if (delay == 0)
                            executor.submit(this);
                        // Schedule to retry after delay
                        else
                            executor.schedule(this, delay, TimeUnit.MILLISECONDS);
                    }
                    catch (Exception e2) {
                        future.setException(e2);    // TODO: Do we want e2 or e?
                    }
                }
                return null;
            }
        });
        future.setFuture(internalFuture);
        return future;
    }
}
