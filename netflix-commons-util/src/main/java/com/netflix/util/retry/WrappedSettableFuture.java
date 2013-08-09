package com.netflix.util.retry;

import java.util.concurrent.Future;

import com.google.common.util.concurrent.AbstractFuture;
import com.sun.istack.internal.Nullable;

public class WrappedSettableFuture<V> extends AbstractFuture<V> {
    private Future<Void> future;
    
    public static <V> WrappedSettableFuture<V> create() {
        return new WrappedSettableFuture<V>();
    }
    
    private WrappedSettableFuture() {}
    
    public void setFuture(Future<Void> future) {
        this.future = future;
    }
    
    @Override
    public boolean set(@Nullable V value) {
        return super.set(value);
    }

    @Override
    public boolean setException(Throwable throwable) {
        return super.setException(throwable);
    }    
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        future.cancel(mayInterruptIfRunning);
        return super.cancel(mayInterruptIfRunning);
    }
}