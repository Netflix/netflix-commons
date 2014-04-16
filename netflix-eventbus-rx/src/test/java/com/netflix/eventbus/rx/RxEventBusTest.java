package com.netflix.eventbus.rx;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;

import com.netflix.eventbus.impl.EventBusImpl;
import com.netflix.eventbus.spi.EventBus;

public class RxEventBusTest {
    @Test
    public void testStream() throws InterruptedException {
        EventBus eventBus = new EventBusImpl();
        RxEventBus rxEventBus = new RxEventBus(eventBus);
        
        final CountDownLatch completion = new CountDownLatch(1);
        final CountDownLatch counter = new CountDownLatch(10);
        
        Subscription sub = rxEventBus.asObservable(Long.class)
            .doOnCompleted(new Action0() {
                @Override
                public void call() {
                    System.out.println("Done");
                    completion.countDown();
                }
            })
            .subscribe(new Action1<Long>() {
                @Override
                public void call(Long t1) {
                    System.out.println(t1);
                    counter.countDown();
                }
            });
        
        for (long i = 0; i < 10; i++) {
            eventBus.publish(i);
        }
        
        Assert.assertTrue(counter.await(1, TimeUnit.SECONDS));
        
        sub.unsubscribe();
        
        Assert.assertTrue(completion.await(1, TimeUnit.SECONDS));
        
    }
}
