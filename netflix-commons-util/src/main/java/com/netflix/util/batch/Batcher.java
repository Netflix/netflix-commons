package com.netflix.util.batch;

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
