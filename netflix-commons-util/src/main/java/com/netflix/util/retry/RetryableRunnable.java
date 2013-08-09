package com.netflix.util.retry;

public interface RetryableRunnable extends Runnable {
    public long nextBackoffDelay();
}
