package com.netflix.util.batch;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.util.concurrent.UnownedScheduledExecutorService;

/**
 * Half sync / Half async batching policy which may call the callback either from a caller thread
 * or from a background thread that flushes the batch periodically.  For simplicity the batch
 * is flushed on a fixed internal.  As a result it's possible to have smaller batches flushed 
 * immediately after a batch without waiting for the full flusherPeriod time since the elements
 * were added to the batcher.
 * 
 * @author elandau
 *
 */
public class HshaTimeAndSizeBatchingPolicy implements BatchingPolicy {

    public static final int DEFAULT_BATCH_SIZE          = 10;
    public static final int DEFAULT_FLUSH_PERIOD_MILLIS = 1000;
    
    public static class Builder {
        private int batchSize   = DEFAULT_BATCH_SIZE;
        private int flushPeriod = DEFAULT_FLUSH_PERIOD_MILLIS;
        
        public Builder withBatchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }
        
        public Builder withFlushPeriod(int flushPeriod) {
            this.flushPeriod = flushPeriod;
            return this;
        }
        
        public HshaTimeAndSizeBatchingPolicy build() {
            return new HshaTimeAndSizeBatchingPolicy(batchSize, flushPeriod, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Call the callback once the batch has batchSizes elements
     */
    private final int    batchSize;
    
    /**
     * Period for flushing the batch.  
     */
    private final long   flushPeriod;
    
    /**
     * Shared executor to be used by all batchers created by the policy
     */
    private final ScheduledExecutorService sharedExecutor;
    
    /**
     * Construct a policy which will result in a single SchedueldExecutorService
     * created for each Batcher instance.
     * @param batchSize
     * @param flushPeriod
     * @param units
     */
    public HshaTimeAndSizeBatchingPolicy(int batchSize, long flushPeriod, TimeUnit units) {
        this(batchSize, flushPeriod, units, null);
    }
    
    /**
     * Construct a policy which will reuse the same ScheduledExecutorService.  Note that
     * for this use case the callback should never block since doing so may block other
     * batchers.
     * 
     * @param batchSize
     * @param flushPeriod
     * @param units
     * @param sharedExecutor
     */
    public HshaTimeAndSizeBatchingPolicy(int batchSize, long flushPeriod, TimeUnit units, ScheduledExecutorService sharedExecutor) {
        this.flushPeriod       = TimeUnit.MILLISECONDS.convert(flushPeriod, units);
        this.batchSize      = batchSize;
        this.sharedExecutor = sharedExecutor;
    }

    @Override
    public <T> Batcher<T> create(final Function<List<T>, Boolean> callback) {
        return new Batcher<T>() {
            private final ScheduledExecutorService     executor;
            private       List<T>                      batch;
            private final ReentrantLock                lock = new ReentrantLock();
    
            {
                Preconditions.checkArgument(batchSize > 1,    "Batch size must be > 1");
                Preconditions.checkArgument(flushPeriod  > 0, "Flush period must be > 0");
                
                this.batch      = Lists.newArrayList();
                
                if (sharedExecutor == null) {
                    executor = Executors.newSingleThreadScheduledExecutor(
                            new ThreadFactoryBuilder()
                            .setDaemon(true)
                            .setNameFormat("Batcher-%d")
                            .build());
                }
                else {
                    executor = new UnownedScheduledExecutorService(sharedExecutor);
                }
                
                executor.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        flush();
                    }
                }, flushPeriod, flushPeriod, TimeUnit.MILLISECONDS);
            }
    
            @Override
            public void add(T object) {
                // Add to batch under a lock
                lock.lock();
                batch.add(object);
                
                // Reached max batch size so pass the batch to the callback
                if (batch.size() >= batchSize) {
                    // Swap for a new empty batch
                    List<T> tempBatch = this.batch;
                    this.batch = Lists.newArrayListWithCapacity(batchSize);
                    lock.unlock();
                    
                    // Call the batch not under a lock
                    callback.apply(tempBatch);
                }
                else {
                    lock.unlock();
                }
            }
    
            @Override
            public void flush() {
                lock.lock();
                if (!batch.isEmpty()) {
                    // Swap for a new empty batch
                    List<T> tempBatch = this.batch;
                    this.batch = Lists.newArrayListWithCapacity(batchSize);
                    lock.unlock();
                    
                    // Call the batch not under a lock
                    callback.apply(tempBatch);
                }
                else {
                    lock.unlock();
                }
            }
    
            @Override
            public void shutdown() {
                executor.shutdown();
            }

            @Override
            public void add(List<T> batch) {
                callback.apply(batch);
            }
        };
    }
}
