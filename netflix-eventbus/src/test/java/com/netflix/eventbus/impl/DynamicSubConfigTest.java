package com.netflix.eventbus.impl;

import com.netflix.eventbus.spi.Subscribe;
import com.netflix.eventbus.spi.SubscriberConfigProvider;
import junit.framework.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Nitesh Kant
 */
public class DynamicSubConfigTest {

    @Test
    public void testDynamicSub() throws Exception {
        EventBusImpl eventBus = new EventBusImpl();
        eventBus.registerSubscriber(new AwesomeSub());

        Set<EventConsumer> stringConsumers = eventBus.getEventConsumer(String.class);
        Assert.assertFalse("No event subscribers for string event found.", stringConsumers.isEmpty());
        SubscriberConfigProvider.SubscriberConfig sub1Config = stringConsumers.iterator().next().getSubscriberConfig();
        Assert.assertEquals("Wrong string consumer batching strategy", Subscribe.BatchingStrategy.None, sub1Config.getBatchingStrategy());
        Assert.assertEquals("Wrong string consumer batch age", 10, sub1Config.getBatchAge());
        Assert.assertEquals("Wrong string consumer batch size", 10, sub1Config.getBatchSize());
        Assert.assertEquals("Wrong string consumer queue size", 10, sub1Config.getQueueSize());


        Set<EventConsumer> doubleConsumers = eventBus.getEventConsumer(Double.class);
        Assert.assertFalse("No event subscribers for double event found.", doubleConsumers.isEmpty());
        SubscriberConfigProvider.SubscriberConfig sub2Config = doubleConsumers.iterator().next().getSubscriberConfig();
        Assert.assertEquals("Wrong string consumer batching strategy", Subscribe.BatchingStrategy.Age, sub2Config.getBatchingStrategy());
        Assert.assertEquals("Wrong string consumer batch age", 20, sub2Config.getBatchAge());
        Assert.assertEquals("Wrong string consumer batch size", 20, sub2Config.getBatchSize());
        Assert.assertEquals("Wrong string consumer queue size", 2000, sub2Config.getQueueSize());

    }

    private static class AwesomeSub implements SubscriberConfigProvider {

        private static final String SUB1_NAME = "sub1";
        private static final String SUB2_NAME = "sub2";


        private static Map<String, SubscriberConfig> configs;

        static {
            configs = new HashMap<String, SubscriberConfig>();
            TestSubscriberConfig sub1Config = new TestSubscriberConfig(Subscribe.BatchingStrategy.None, 10, 10, 10, true);
            TestSubscriberConfig sub2Config = new TestSubscriberConfig(Subscribe.BatchingStrategy.Age, 20, 20, 2000, false);
            configs.put(SUB1_NAME, sub1Config);
            configs.put(SUB2_NAME, sub2Config);
        }

        @Override
        public SubscriberConfig getConfigForName(String subscriberName) {
            return configs.get(subscriberName);
        }

        @Subscribe(name = SUB1_NAME)
        public void sub1(String s) {

        }

        @Subscribe(name = SUB2_NAME)
        public void sub2(Double s) {

        }

        private static class TestSubscriberConfig implements SubscriberConfig {

            private final Subscribe.BatchingStrategy strategy;
            private final int age;
            private final int size;
            private final int qSize;
            private final boolean sync;

            private TestSubscriberConfig(Subscribe.BatchingStrategy strategy, int age, int size, int qSize,
                                         boolean sync) {
                this.strategy = strategy;
                this.age = age;
                this.size = size;
                this.qSize = qSize;
                this.sync = sync;
            }

            @Override
            public Subscribe.BatchingStrategy getBatchingStrategy() {
                return strategy;
            }

            @Override
            public int getBatchAge() {
                return age;
            }

            @Override
            public int getBatchSize() {
                return size;
            }

            @Override
            public int getQueueSize() {
                return qSize;
            }

            @Override
            public boolean syncIfAllowed() {
                return sync;
            }
        }
    }
}
