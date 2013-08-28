package com.netflix.util.concurrent.policy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Concurrent policy that creates a dedicated thread pool.
 * Note that any change to the policy will only affect newly
 * created executors.
 * 
 * @author elandau
 *
 */
public class FixedPoolConcurrencyPolicy implements ConcurrencyPolicy {
    public final int DEFAULT_POOL_SIZE = 1;
    
    private int poolSize = DEFAULT_POOL_SIZE;
    
    public int getPoolSize() {
        return poolSize;
    }
    
    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    @Override
    public ExecutorService getExecutorService(String name) {
        return Executors.newFixedThreadPool(
                poolSize, 
                new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat(name + "-%d")
                    .build());
    }

}
