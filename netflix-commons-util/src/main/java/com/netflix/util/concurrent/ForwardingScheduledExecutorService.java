package com.netflix.util.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.ForwardingObject;

public abstract class ForwardingScheduledExecutorService extends ForwardingObject
    implements ScheduledExecutorService {
    /** Constructor for use by subclasses. */
    protected ForwardingScheduledExecutorService() {}
    
    @Override
    protected abstract ScheduledExecutorService delegate();
    
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
      throws InterruptedException {
        return delegate().awaitTermination(timeout, unit);
    }
    
    @Override
    public <T> List<Future<T>> invokeAll(
      Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate().invokeAll(tasks);
    }
    
    @Override
    public <T> List<Future<T>> invokeAll(
      Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
        return delegate().invokeAll(tasks, timeout, unit);
    }
    
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
        return delegate().invokeAny(tasks);
    }
    
    @Override
    public <T> T invokeAny(
      Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
        return delegate().invokeAny(tasks, timeout, unit);
    }
    
    @Override
    public boolean isShutdown() {
        return delegate().isShutdown();
    }
    
    @Override
    public boolean isTerminated() {
        return delegate().isTerminated();
    }
    
    @Override
    public void shutdown() {
        delegate().shutdown();
    }
    
    @Override
    public List<Runnable> shutdownNow() {
        return delegate().shutdownNow();
    }
    
    @Override
    public void execute(Runnable command) {
        delegate().execute(command);
    }
    
    public <T> Future<T> submit(Callable<T> task) {
        return delegate().submit(task);
    }
    
    @Override
    public Future<?> submit(Runnable task) {
        return delegate().submit(task);
    }
    
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate().submit(task, result);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable arg0, long arg1, TimeUnit arg2) {
        return delegate().schedule(arg0, arg1, arg2);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> arg0, long arg1,
            TimeUnit arg2) {
        return delegate().schedule(arg0, arg1, arg2);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable arg0, long arg1,
            long arg2, TimeUnit arg3) {
        return delegate().scheduleAtFixedRate(arg0, arg1, arg2, arg3);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable arg0, long arg1,
            long arg2, TimeUnit arg3) {
        return delegate().scheduleWithFixedDelay(arg0, arg1, arg2, arg3);
    }
}
