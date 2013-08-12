package com.netflix.util.batch;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.google.common.base.Function;

/**
 * Batcher callback that adds the batch to a blocking queue
 * 
 * @author elandau
 *
 * @param <T>
 */
public class BatchToQueueFunction<T> implements Function<List<T>, Boolean> {
    /**
     * Target queue
     */
    private final BlockingQueue<List<T>> queue;
    
    public static <T> BatchToQueueFunction<T> wrap(BlockingQueue<List<T>> queue) {
        return new BatchToQueueFunction<T>(queue);
    }
    
    public BatchToQueueFunction(BlockingQueue<List<T>> queue) {
        this.queue = queue;
    }
    
    @Override
    public Boolean apply(List<T> batch) {
        try {
            queue.put(batch);
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

}
