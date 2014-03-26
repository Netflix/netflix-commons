package com.netflix.lifecycle.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class CoreCountBasedScheduledExecutorServiceProviderTest {
    @Test
    public void shouldCallAndShutdown() throws Exception {
        CoreCountBasedScheduledExecutorServiceProvider provider = new CoreCountBasedScheduledExecutorServiceProvider();
        final CountDownLatch latch = new CountDownLatch(2);
        ScheduledFuture<?> future = provider.get().scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        },
        100, 100, TimeUnit.MILLISECONDS);
        
        Assert.assertFalse(future.isDone());
        latch.await();
        Assert.assertFalse(future.isDone());
        
        provider.shutdown();
        
        Assert.assertTrue(future.isDone());
        
        
    }
    
    @Test
    public void shouldNotCrashOnUnusedShutdown() {
        CoreCountBasedScheduledExecutorServiceProvider provider = new CoreCountBasedScheduledExecutorServiceProvider();
        provider.shutdown();
    }
}
