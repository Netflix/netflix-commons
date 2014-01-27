package com.netflix.eventbus;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.eventbus.impl.EventBusImpl;
import com.netflix.eventbus.spi.EventBus;

public class AbstractEventBusBridgeTest {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractEventBusBridgeTest.class);
    
    @Test
    public void testString() throws Exception {
        EventBus eventBus = new EventBusImpl();
        
        DummyEventBusBridge bridge = DummyEventBusBridge.builder()
            .withEventBus(eventBus)
            .withEventType(String.class)
            .withExpectedCount(1)
            .build();
        
        eventBus.publish(new String("Foo"));
        Assert.assertTrue(bridge.await(3,  TimeUnit.SECONDS));        
        Assert.assertEquals(1, bridge.getConsumeCount());
        Assert.assertEquals(0, bridge.getConsumeErrorCount());
    }   
    
    @Test
    public void testConsumeErrorStats() throws Exception {
        EventBus eventBus = new EventBusImpl();
        
        final RuntimeException e = new RuntimeException("Suro failed to send the message");
        
        DummyEventBusBridge bridge = DummyEventBusBridge.builder()
            .withEventBus(eventBus)
            .withEventType(String.class)
            .build();

        bridge.setError(e);
        eventBus.publish(new String("Foo"));
        
        TimeUnit.SECONDS.sleep(1);
        Assert.assertEquals(0, bridge.getConsumeCount());
        Assert.assertEquals(1, bridge.getConsumeErrorCount());
        Assert.assertEquals(e, bridge.getLastConsumeException());
    }
    
    @Test
    public void testGetStatusAreImmutable() throws Exception {
        EventBus eventBus = new EventBusImpl();
        DummyEventBusBridge bridge = DummyEventBusBridge.builder()
            .withEventBus(eventBus)
            .withEventType(String.class)
            .build();
        
        try {
            bridge.getStats().incConsumeCount();
            Assert.fail("Stats should be immutable");
        }
        catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testPauseAndResume() throws Exception {
        EventBus eventBus = new EventBusImpl();
        DummyEventBusBridge bridge = DummyEventBusBridge.builder()
            .withExpectedCount(2)
            .withEventBus(eventBus)
            .withEventType(String.class)
            .build();
        
        eventBus.publish(new String("Foo"));
        Assert.assertTrue(waitForConsumeCount(bridge, 1, 1, TimeUnit.SECONDS));
        
        bridge.pause();
        eventBus.publish(new String("Foo"));
        Assert.assertFalse(waitForConsumeCount(bridge, 2, 1, TimeUnit.SECONDS));
        
        bridge.resume();
        eventBus.publish(new String("Foo"));
        Assert.assertTrue(waitForConsumeCount(bridge, 2, 1, TimeUnit.SECONDS));
    }
    
    public boolean waitForConsumeCount(DummyEventBusBridge bridge, long expected, long delay, TimeUnit units) throws Exception {
        long intervals = TimeUnit.MILLISECONDS.convert(delay, units)/100;
        assert intervals > 0;
        for (long i = 0; i < intervals; i++) {
            if (bridge.getConsumeCount() == expected)
                return true;
            TimeUnit.MILLISECONDS.sleep(100);
        }
        
        return false;
    }
}
