package com.netflix.eventbus.bridge;

/**
 * Interface specifying stats exposed by the EventBusSuroBridge.
 * 
 * @author elandau
 *
 */
public interface EventBusBridgeStats {
    /**
     * @return Return number of successfully bridged events
     */
    public long getConsumeCount();
    
    /**
     * @return Return number of failed consume event calls
     */
    public long getConsumeErrorCount();

    /**
     * Called for each successful consume
     * @return New count of consumed events
     */
    public long incConsumeCount();
    
    /**
     * Called for each failed consume
     * @param e - Exception describing reason for failure
     * @return New count of consume errors
     */
    public long incConsumeErrorCount(Exception e);
    
    /**
     * @return Last exception provided to {@link incConsumeErrorCount}
     */
    public Exception getLastConsumeException();
}
