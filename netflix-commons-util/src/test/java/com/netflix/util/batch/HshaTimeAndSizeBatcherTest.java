package com.netflix.util.batch;

import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

public class HshaTimeAndSizeBatcherTest {
    @Test
    public void testBySize() throws Exception {
        final List<String> result = Lists.newArrayList();
        
        BatchingPolicy policy = new HshaTimeAndSizeBatchingPolicy(2, 1, TimeUnit.SECONDS);
        Batcher<String> batcher = policy.create(new Function<List<String>, Boolean>() {
            public Boolean apply(List<String> list) {
                result.addAll(list);
                return true;
            }
        });
        
        batcher.add("A");
        batcher.add("B");
        Thread.sleep(100);
        batcher.shutdown();
     
        Assert.assertEquals(2, result.size());
    }
    
    @Test
    public void testByTime() throws Exception {
        final List<String> result = Lists.newArrayList();
        
        BatchingPolicy policy = new HshaTimeAndSizeBatchingPolicy(10, 1, TimeUnit.SECONDS);
        Batcher<String> batcher = policy.create(new Function<List<String>, Boolean>() {
            public Boolean apply(List<String> list) {
                result.addAll(list);
                return true;
            }
        });
        
        batcher.add("A");
        batcher.add("B");
        
        Assert.assertEquals(0, result.size());
        
        Thread.sleep(2010);
        
        Assert.assertEquals(2, result.size());
    }
    
    @Test
    public void testByTimeWithBlockingQueue() throws Exception {
        BlockingQueue<List<String>> queue = Queues.newLinkedBlockingQueue();
        BatchingPolicy policy = new HshaTimeAndSizeBatchingPolicy(10, 1, TimeUnit.SECONDS);
        Batcher<String> batcher = policy.create(BatchToQueueFunction.wrap(queue));
        
        batcher.add("A");
        batcher.add("B");
        
        Assert.assertEquals(null, queue.poll());
        
        List<String> batch = queue.take();
        
        Assert.assertEquals(2, batch.size());
    }
}
