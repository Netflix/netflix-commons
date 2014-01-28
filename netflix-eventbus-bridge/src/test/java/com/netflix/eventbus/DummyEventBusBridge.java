package com.netflix.eventbus;

import com.netflix.eventbus.bridge.AbstractEventBusBridge;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

public class DummyEventBusBridge extends AbstractEventBusBridge {

    public static class Builder extends AbstractEventBusBridge.Builder<Builder> {
        private CountDownLatch latch;
        
        public Builder withExpectedCount(int count) {
            latch = new CountDownLatch(count);
            return this;
        }
        
        @Override
        protected Builder self() {
            return this;
        }
        
        public DummyEventBusBridge build() throws Exception {
            validate();
            return new DummyEventBusBridge(this);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    private final CountDownLatch latch;
    private final AtomicLong counter = new AtomicLong();
    private Exception forcedError;
    
    protected DummyEventBusBridge(final Builder init)
            throws Exception {
        super(init);
        this.latch = init.latch;
        init();
    }

    public boolean await(long timeout, TimeUnit units) throws Exception {
        return latch.await(timeout, units);
    }

    @Override
    protected void sendEvent(Object event) throws Exception {
        if (forcedError != null)
            throw forcedError;
        counter.incrementAndGet();
        latch.countDown();
    }
    
    public void setError(Exception forcedError) {
        this.forcedError = forcedError;
    }
}
