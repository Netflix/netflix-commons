package com.netflix.util.concurrent.policy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Concurrent policy that creates a dedicated single thread pool 
 * 
 * @author elandau
 *
 */
public class SingleThreadedConcurrencyPolicy implements ConcurrencyPolicy {
    @Override
    public ExecutorService getExecutorService(String name) {
        return Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat(name + "-%d")
                    .build());
    }
}

