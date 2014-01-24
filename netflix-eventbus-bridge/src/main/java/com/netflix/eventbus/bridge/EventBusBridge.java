package com.netflix.eventbus.bridge;

/**
 * Base API for creating a bridge between the EventBus and any other
 * messaging API.  A bridge differs from a Subscriber or event handler
 * in that it's purpose is to forward messages rather than doing any
 * real processing of the message
 * 
 * @author elandau
 */
public interface EventBusBridge {
    /**
     * Pause processing messages.  Any messages being send to the 
     * bridge will be discarded.
     * 
     * No error is thrown if already paused.
     * @throws Exception 
     */
    public void pause() throws Exception;
    
    /**
     * Resume processing messages.  
     * 
     * 
     * No error is thrown if already resumed
     */
    public void resume() throws Exception;
}
