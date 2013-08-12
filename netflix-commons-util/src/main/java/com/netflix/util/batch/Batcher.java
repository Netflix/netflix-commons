package com.netflix.util.batch;

import java.util.List;

/**
 * Abstraction for an object batcher instance.  An implementation of Batcher 
 * will accumulate objects in a list and provide the list to a callback once 
 * a batch has been constructed based on the batching strategy.  A batcher 
 * is constructed and associated with the callback by calling a BatchingPolicy
 * create() method.
 * 
 * @author elandau
 *
 * @param <T>
 */
public interface Batcher<T> {
    /**
     * Send a user defined batch through the batcher.  This will result in 
     * any pending batch being flushed to the callback as well as this batch
     * being send directly to the callback
     * @param batch
     */
    public void add(List<T> batch);
    
    /**
     * Add an object to the batch.  This call may result in a synchronous call to the 
     * batch callback.
     * 
     * @param object
     */
    public void add(T object);
    
    /**
     * Flush any pending objects to the batcher callback regardless of strategy.
     */
    public void flush();
    
    /**
     * Shutdown the batcher, including any running internal threads
     */
    public void shutdown();
}
