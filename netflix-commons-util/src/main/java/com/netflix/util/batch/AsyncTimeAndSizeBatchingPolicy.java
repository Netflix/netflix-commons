package com.netflix.util.batch;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import com.netflix.util.concurrent.UnownedExecutorService;

/**
 * Batching strategy using a single queue and thread to batch up to N messages
 * while waiting no more than waitTime 
 * 
 * @author elandau
 */
public class AsyncTimeAndSizeBatchingPolicy implements BatchingPolicy {

    public static class Entry<T> {
        long expiration;
        T    entity;
        
        public Entry(T entity, long expiration) {
            this.expiration = expiration;
            this.entity     = entity;
        }
    }
    
    public class EntryToEntityFunction<T> implements Function<Entry<T>, T> {
        @Override
        public T apply(Entry<T> input) {
            return input.entity;
        }
    }


    private final int    batchSize;
    private final long   maxDelay;
    private final ExecutorService sharedExecutor;
    
    public AsyncTimeAndSizeBatchingPolicy(int batchSize, long maxDelay, TimeUnit units) {
        this(batchSize, maxDelay, units, null);
    }

    public AsyncTimeAndSizeBatchingPolicy(int batchSize, long maxDelay, TimeUnit units, ExecutorService sharedExecutor) {
        this.maxDelay   = TimeUnit.NANOSECONDS.convert(maxDelay, units);
        this.batchSize  = batchSize;
        this.sharedExecutor = sharedExecutor;
    }

    @Override
    public <T> Batcher<T> create(final Function<List<T>, Boolean> callback) {
        return new Batcher<T>() {
            private final ExecutorService              executor;
            private final BlockingQueue<Entry<T>>      queue;
            private       List<Entry<T>>               batch;
            
            {
                Preconditions.checkArgument(batchSize > 1, "Batch size must be > 1");
                Preconditions.checkArgument(maxDelay  > 0, "Delay must be > 1");
                
                this.queue      = Queues.newLinkedBlockingDeque();
                this.batch      = Lists.newArrayList();
                
                if (sharedExecutor == null) {
                    executor = Executors.newSingleThreadScheduledExecutor(
                            new ThreadFactoryBuilder()
                            .setDaemon(true)
                            .setNameFormat("Batcher-%d")
                            .build());
                }
                else {
                    executor = new UnownedExecutorService(sharedExecutor);
                }
                
                executor.submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception{
                        consumeEntries();
                        return true;
                    }
                });
            }
    
            @Override
            public void add(T object) {
                queue.add(new Entry<T>(object, System.nanoTime() + maxDelay));
            }
    
            @Override
            public void flush() {
                // TODO
            }
    
            private void consumeEntries() throws Exception {
                long expiration = -1;
                
                while (!Thread.currentThread().isInterrupted()) {
                    Entry<T> entry;
                    if (expiration == -1) {
                        entry = queue.take();
                    }
                    else {
                        // Determine the batch timeout value
                        long waitTime = expiration - System.nanoTime();
                        if (waitTime < 0)
                            waitTime = 0;
                        
                        // Try to get an element and add it to the batch
                        entry = queue.poll(waitTime, TimeUnit.NANOSECONDS);
                    }
                    
                    long now = System.nanoTime();
                    synchronized (this) {
                        // Got one
                        if (entry != null) {
                            batch.add(entry);
                        }
                        
                        // Check if batch needs to be sent
                        if (!batch.isEmpty()) {
                            if (batch.size() == batchSize ||        // Batch size reached
                                batch.get(0).expiration < now) {    // Batch time reached
                                
                                // Make sure we have a full batch in case we hit the time limit
                                if (batch.size() < batchSize) {
                                    queue.drainTo(batch, batchSize - batch.size());
                                }
                                
                                // Update to a future expiration so we don't get into a tight loop
                                expiration = -1;
                                
                                try {
                                    callback.apply(Lists.newArrayList(Collections2.transform(batch, new EntryToEntityFunction<T>())));
                                }
                                catch (Throwable t) {
                                    // TOOD: Pass to an exception handler
                                    t.printStackTrace();
                                }
                                batch.clear();
                            }
                            else if (batch.size() == 1) {
                                expiration = batch.get(0).expiration;
                            }
                        }
                        else {
                            expiration = -1;
                        }
                    }
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
