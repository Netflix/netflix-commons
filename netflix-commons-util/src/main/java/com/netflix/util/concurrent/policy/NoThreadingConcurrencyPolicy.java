package com.netflix.util.concurrent.policy;

import java.util.concurrent.ExecutorService;

import com.netflix.util.concurrent.NoThreadExecutorService;

/**
 * Concurrent policy to use when NO concurrency is desired.  The executor
 * will execute all operations within the caller's thread. 
 * 
 * @author elandau
 *
 */
public class NoThreadingConcurrencyPolicy implements ConcurrencyPolicy {
    @Override
    public ExecutorService getExecutorService(String name) {
        return new NoThreadExecutorService();
    }
}
