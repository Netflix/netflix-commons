package com.netflix.eventbus.bridge;

/**
 * Decorator for {@link EventBusBridgeStats} that makes it unmodifiable.
 * An instance of this is returned by {@link EventBusSuroBridgeStats.getStats()}
 * 
 * @author elandau
 *
 */
public class ImmutableEventBusBridgeStats implements EventBusBridgeStats {

    private EventBusBridgeStats delegate;
    
    public ImmutableEventBusBridgeStats(EventBusBridgeStats delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public long getConsumeCount() {
        return this.delegate.getConsumeCount();
    }

    @Override
    public long getConsumeErrorCount() {
        return this.delegate.getConsumeErrorCount();
    }

    @Override
    public long incConsumeCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long incConsumeErrorCount(Exception e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Exception getLastConsumeException() {
        return this.delegate.getLastConsumeException();
    }
}
