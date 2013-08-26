package com.netflix.util.retry;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Base implementation for a RetryPolicy which implements the interaction
 * between the retry policy and the retry executor service
 *  
 * @author elandau
 *
 */
public abstract class AbstractRetryPolicy implements RetryPolicy {

    /**
     * Capture the context of a single execution
     * @author elandau
     *
     */
    protected class Context {
        private final long      startTime     = System.nanoTime();
        private       long      attemptCount  = 0;
        private       Throwable lastThrowable = null;
        
        public long getAttemptCount() {
            return attemptCount;
        }
        
        public Throwable getLastThrowable() {
            return this.lastThrowable;
        }
        
        public long incrementAttemptCount(Throwable t) {
            this.lastThrowable = t;
            return ++attemptCount;
        }
        
        public long getEllapsedTime(TimeUnit units) {
            return units.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }
    }
    
    
    @Override
    public final <R> ListenableFuture<R> submit(final Callable<R> callable, final ScheduledExecutorService executor) {
        final SettableFuture<R> future = SettableFuture.create();
        final Context context = new Context();
        try {
            // First attempt in the client thread context
            callable.call();
        }
        catch (Throwable t) {
            // Figure out the first backoff delay
            long delay = getBackoffDelay(context);
            if (delay >= 0) {
                context.incrementAttemptCount(t);
                executor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // If the future is cancelled then we can just ignore this event.
                            if (!future.isCancelled()) {
                                future.set(callable.call());
                            }
                            else {
                                future.setException(context.getLastThrowable());
                            }
                        } catch (Throwable t) {
                            context.incrementAttemptCount(t);
                            long delay = getBackoffDelay(context);
                            if (delay >= 0) {
                                executor.schedule(this, delay, TimeUnit.MILLISECONDS);
                            }
                            else {
                                future.setException(t);
                            }
                        }
                    }
                }, delay, TimeUnit.MILLISECONDS);
            }
            else {
                future.setException(t);
            }
        }
        return future;
    }
    
    protected Context createContext() {
        return new Context();
    }
    
    /**
     * Calculate the next backoff delay based on the provided context
     * @param context
     * @return
     */
    protected abstract long getBackoffDelay(Context context);
    
}
