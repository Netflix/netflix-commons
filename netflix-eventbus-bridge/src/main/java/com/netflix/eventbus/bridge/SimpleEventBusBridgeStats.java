package com.netflix.eventbus.bridge;

import java.util.concurrent.atomic.AtomicLong;

public class SimpleEventBusBridgeStats implements EventBusBridgeStats {
    private final AtomicLong consumeErrorCount = new AtomicLong(0);
    private final AtomicLong consumeCount = new AtomicLong(0);
    private volatile Exception lastConsumeException = null;
    
    @Override
    public long getConsumeCount() {
        return consumeCount.get();
    }

    @Override
    public long getConsumeErrorCount() {
        return consumeErrorCount.get();
    }

    @Override
    public long incConsumeCount() {
        return consumeCount.incrementAndGet();
    }

    @Override
    public long incConsumeErrorCount(Exception e) {
        this.lastConsumeException = e;
        return this.consumeErrorCount.incrementAndGet();
    }

    @Override
    public Exception getLastConsumeException() {
        return this.lastConsumeException;
    }
}
