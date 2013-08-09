package com.netflix.util.retry;

/**
 * Retry policy that implements no retry at all.
 * 
 * @author elandau
 */
public class NoRetryPolicy implements RetryPolicy {
    @Override
    public long nextBackoffDelay(int attempt, long elapsedMillis) {
        return -1;
    }
}
