package com.netflix.util.concurrent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ForwardingExecutorService;

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
public class UnownedExecutorService extends ForwardingExecutorService {
    private final ExecutorService delegate;
    
    public UnownedExecutorService(ExecutorService delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        //  return true;
        throw new UnsupportedOperationException();
    }
    
    @Override
    protected ExecutorService delegate() {
        return delegate;
    }

    public void shutdown() {
    }

    @Override
    public List<Runnable> shutdownNow() {
        return ImmutableList.of();
    }

}
