package com.netflix.util.concurrent;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;

/**
 * Executor service used when sharing an executor among different 
 * clients and protects the executor from being shut down by a client.
 * 
 * TODO: We may want to wrap and keep track of submitted tasks to 
 *       implement proper shutdown.  Currently shutdown is essentially
 *       a no op and may result in undesireable behavior.
 * 
 * @author elandau
 *
 */
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
