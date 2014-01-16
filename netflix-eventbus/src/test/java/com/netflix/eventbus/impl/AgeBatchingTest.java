package com.netflix.eventbus.impl;

import com.netflix.eventbus.spi.Subscribe;
import com.netflix.eventbus.spi.SubscriberConfigProvider;
import com.netflix.eventbus.utils.EventBusUtils;
import junit.framework.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Nitesh Kant (nkant@netflix.com)
 */
public class AgeBatchingTest {

    @Test
    public void testAgeBatchAged() throws Exception {
        MySub mySub = new MySub();
        Method subMethod = mySub.getClass().getMethod("subMe", String.class);
        SubscriberConfigProvider.SubscriberConfig subscriberConfig = EventBusUtils.getSubscriberConfig(subMethod, mySub);
        AgeBatchingQueue q = newQ(subMethod, subscriberConfig);
        Assert.assertTrue("Age batch queue offer failed.", q.offer("Event1"));
        q.invokeReaping();
        AgeBatchingQueue.AgeBatch agedBatch = q.blockingTakeWithTimeout(subscriberConfig.getBatchAge());
        Assert.assertNotNull("No batch available after batch age expired.", agedBatch);
    }

    @Test
    public void testAgeBatchYoung() throws Exception {
        MySub mySub = new MySub();
        Method subMethod = mySub.getClass().getMethod("subMeLong", String.class);
        SubscriberConfigProvider.SubscriberConfig subscriberConfig = EventBusUtils.getSubscriberConfig(subMethod, mySub);
        AgeBatchingQueue q = newQ(subMethod, subscriberConfig);
        String event = "Event1";
        Assert.assertTrue("Age batch queue offer failed.", q.offer(event));

        AgeBatchingQueue.AgeBatch currentBatch = q.getCurrentBatch();
        Assert.assertTrue("Offered event not in current batch", currentBatch.events.contains(event));

        Object shdBeNull = q.nonBlockingTake();
        Assert.assertNull("Batch available before batch age expiry.", shdBeNull);
    }

    @Test
    public void testQueueFull() throws Exception {
        MySub mySub = new MySub();
        Method subMethod = mySub.getClass().getMethod("subMe", String.class);
        SubscriberConfigProvider.SubscriberConfig subscriberConfig = EventBusUtils.getSubscriberConfig(subMethod, mySub);
        AgeBatchingQueue q = newQ(subMethod, subscriberConfig);
        for (int j=0; j < 3;j++) {
            for (int i = 0; i < 10; i++) {
                Assert.assertTrue("Age batch queue offer failed.", q.offer("event" + j +i));
            }
            q.invokeReaping();
        }

        Assert.assertFalse("Offer not failing when queue is full", q.offer("EventToFail"));
        Assert.assertNotNull("No batch available after age expiry.", q.blockingTake());
        Assert.assertNotNull("No batch available after age expiry.", q.blockingTake());
        Assert.assertTrue("Offer failing when queue is not full", q.offer("EventToGoThrough"));

    }

    private AgeBatchingQueue newQ(Method subMethod, SubscriberConfigProvider.SubscriberConfig annotation) {
        return new AgeBatchingQueue(subMethod, annotation, false, new AtomicLong());
    }

    public class MySub {

        @Subscribe(batchAge = 1000, queueSize = 2,batchingStrategy = Subscribe.BatchingStrategy.Age)
        public void subMe(String event) {
            System.out.println("AgeBatchingTest$MySub.subMe");
        }

        @Subscribe(batchAge = 60000, queueSize = 2,batchingStrategy = Subscribe.BatchingStrategy.Age)
        public void subMeLong(String event) {
            System.out.println("AgeBatchingTest$MySub.subMe");
        }
    }
}
