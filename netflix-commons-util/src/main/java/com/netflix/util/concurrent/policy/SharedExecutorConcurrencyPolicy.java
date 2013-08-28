package com.netflix.util.concurrent.policy;

import java.util.concurrent.ExecutorService;

import com.netflix.util.concurrent.UnownedExecutorService;

/**
 * ConcurrencyPolicy that reuses the same executor.  The returned 
 * executor is protected from being shutdown.  
 * 
 * @see UnownedExecutorService
 * @author elandau
 */
public class SharedExecutorConcurrencyPolicy implements ConcurrencyPolicy {
    private final ExecutorService executor;
    
    public SharedExecutorConcurrencyPolicy(ExecutorService executor) {
        this.executor = executor;
    }
    
    @Override
    public ExecutorService getExecutorService(String name) {
        return new UnownedExecutorService(executor);
    }
}
