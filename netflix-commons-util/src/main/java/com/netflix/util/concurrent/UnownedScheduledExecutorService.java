package com.netflix.util.concurrent;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;

public class UnownedScheduledExecutorService extends ForwardingScheduledExecutorService {
    private final ScheduledExecutorService delegate;
    
    public UnownedScheduledExecutorService(ScheduledExecutorService delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return true;
    }
    
    @Override
    protected ScheduledExecutorService delegate() {
        return delegate;
    }

    public void shutdown() {
    }

    @Override
    public List<Runnable> shutdownNow() {
        return ImmutableList.of();
    }
}
