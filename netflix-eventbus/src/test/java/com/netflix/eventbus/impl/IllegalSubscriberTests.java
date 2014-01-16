package com.netflix.eventbus.impl;

import com.netflix.eventbus.spi.InvalidSubscriberException;
import com.netflix.eventbus.spi.Subscribe;
import org.junit.Test;

/**
 * @author Nitesh Kant (nkant@netflix.com)
 */
public class IllegalSubscriberTests {

    @Test
    public void testMultiArgumentsSubscriber() throws Exception {
        EventBusImpl bus = new EventBusImpl(null);
        Object multiArgSub = new Object() {

            @Subscribe
            public void subscribe(String s, String s1) {

            }
        };
        registerInvalidSub(bus, multiArgSub, "Subscriber with two arguments not invalid!");
    }

    @Test
    public void testIllegalBatchSubscriber() throws Exception {
        EventBusImpl bus = new EventBusImpl(null);
        Object multiArgSub = new Object() {

            @Subscribe(batchingStrategy = Subscribe.BatchingStrategy.Age)
            public void subscribe(String s) {

            }
        };
        registerInvalidSub(bus, multiArgSub, "Batching subscriber with non-iterable argument not invalid!");
    }

    @Test
    public void testNoBatchAgeSubscriber() throws Exception {
        EventBusImpl bus = new EventBusImpl(null);
        Object multiArgSub = new Object() {

            @Subscribe(batchingStrategy = Subscribe.BatchingStrategy.Age, batchSize = 100)
            public void subscribe(Iterable s) {

            }
        };
        registerInvalidSub(bus, multiArgSub, "Batching subscriber with no batch age not invalid!");
    }

    @Test
    public void testNoBatchSizeSubscriber() throws Exception {
        EventBusImpl bus = new EventBusImpl(null);
        Object multiArgSub = new Object() {

            @Subscribe(batchingStrategy = Subscribe.BatchingStrategy.SizeOrAge, batchAge = 100)
            public void subscribe(Iterable s) {

            }
        };
        registerInvalidSub(bus, multiArgSub, "Batching subscriber with no batch age not invalid!");
    }

    private void registerInvalidSub(EventBusImpl bus, Object multiArgSub, String errorMessage) throws InvalidSubscriberException {
        try {
            bus.registerSubscriber(multiArgSub);
            throw new AssertionError(errorMessage);
        } catch (InvalidSubscriberException e) {
            // expected.
        }
    }
}
