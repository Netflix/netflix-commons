package com.netflix.util.batch;

import java.util.List;

import com.google.common.base.Function;

/**
 * Abstraction for a strategy to batch objects.  Once a batch has been constructed
 * based on the strategy logic it will be sent to the callback.  Note that no 
 * assumptions may be made regarding which thread makes the actual call to the
 * callback.
 * 
 * @author elandau
 *
 */
public interface BatchingPolicy {
    /**
     * Create a batcher context
     * 
     * @param callback
     * @return
     */
    public <T> Batcher<T> create(Function<List<T>, Boolean> callback);
}
