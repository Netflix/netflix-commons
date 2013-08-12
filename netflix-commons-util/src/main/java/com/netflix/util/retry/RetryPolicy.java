package com.netflix.util.retry;

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
     * Implement the retry policy logic based on the provided number of attempts 
     * and elapsed time.
     * 
     * @param attempt
     * @param elapsedMillis
     * @return
     */
    public long nextBackoffDelay(int attempt, long elapsedMillis);
}
