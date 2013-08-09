package com.netflix.util.retry;

/**
 * Abstraction for a RetryPolicy that determines the number of retries and backoff
 * amount based on the number of retry attempts and elapsed time since an 
 * operation was started.
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
