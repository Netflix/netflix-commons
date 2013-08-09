package com.netflix.util.retry;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ListenableFuture;

public class TestRetryPolicy {
    @Test
    public void testSimpleCountingRetry() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        
        CountingRetryPolicy policy = new CountingRetryPolicy(5);
        
        final AtomicInteger counter = new AtomicInteger();
        ListenableFuture<Integer> future = Retryables.execute(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                counter.incrementAndGet();
                throw new RuntimeException("Blah blah");
            }
        }, policy, executor);
        
        try {
            int result = future.get();
            Assert.fail("Should have thrown an exception");
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
            Assert.assertTrue(e.getMessage().contains("Blah blah"));
        }
    }
    
    @Test
    public void testRetryCancel() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        
        ConstantBackoffRetryPolicy policy = new ConstantBackoffRetryPolicy(5, 10, TimeUnit.SECONDS);
        
        final AtomicInteger counter = new AtomicInteger();
        ListenableFuture<Integer> future = Retryables.execute(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                counter.incrementAndGet();
                throw new RuntimeException("Blah blah");
            }
        }, policy, executor);
        
        try {
            future.cancel(true);
            int result = future.get();
            Assert.fail("Should have thrown an exception");
        } catch (CancellationException e) {
        } catch (ExecutionException e) {
            Assert.fail("Should have gotten a CancellationException");
        } catch (InterruptedException e) {
            Assert.fail("Should have gotten a CancellationException");
        }
    }
    
    @Test
    public void testWithDelay() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        
        ConstantBackoffRetryPolicy policy = new ConstantBackoffRetryPolicy(5, 100, TimeUnit.MILLISECONDS);
        
        final AtomicInteger counter = new AtomicInteger();
        ListenableFuture<Integer> future = Retryables.execute(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                counter.incrementAndGet();
                throw new RuntimeException("Blah blah");
            }
        }, policy, executor);
        
        Stopwatch sw = new Stopwatch().start();
        try {
            int result = future.get();
            Assert.fail("Should have thrown an exception");
        } catch (ExecutionException e) {
        } catch (InterruptedException e) {
        }
        Assert.assertEquals("Should have retried 5 times",  5, counter.get());
        Assert.assertTrue("Should have taken over 400 milliseconds to fail", sw.elapsed(TimeUnit.MILLISECONDS) > 400);
    }
}
