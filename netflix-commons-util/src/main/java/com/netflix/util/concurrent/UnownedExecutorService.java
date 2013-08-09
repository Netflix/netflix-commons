package com.netflix.util.concurrent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ForwardingExecutorService;

public class UnownedExecutorService extends ForwardingExecutorService {
    private final ExecutorService executor;
    
    public UnownedExecutorService(ExecutorService executor) {
        this.executor = executor;
    }
    
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return true;
    }
    
    @Override
    protected ExecutorService delegate() {
        return executor;
    }

    public void shutdown() {
    }

    @Override
    public List<Runnable> shutdownNow() {
        return ImmutableList.of();
    }

}
