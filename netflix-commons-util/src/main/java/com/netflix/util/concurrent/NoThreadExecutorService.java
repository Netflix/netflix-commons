package com.netflix.util.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.Futures;

/**
 * Executor service that is not an actual executor and executes everything in the 
 * caller's thread.   This will be used mainly to have the same code path for sync
 * and async threading models as well as for testing.
 * 
 * @author elandau
 *
 */
public class NoThreadExecutorService implements ExecutorService {
    private volatile boolean isShutdown = false;
    
    @Override
    public void execute(Runnable command) {
        command.run();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        return true;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<Future<T>> invokeAll(
            Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
            long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public boolean isTerminated() {
        return isShutdown;
    }

    @Override
    public void shutdown() {
        isShutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        return null;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        try {
            return Futures.immediateFuture(task.call());
        } catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public Future<?> submit(Runnable task) {
        try {
            task.run();
            return Futures.immediateFuture(true);
        } catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        try {
            task.run();
            return Futures.immediateFuture(result);
        } catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

}
