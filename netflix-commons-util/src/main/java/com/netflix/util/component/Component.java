package com.netflix.util.component;

/**
 * Base interface for any component that has a start/stop/pause/resume states.
 * 
 *                       stop
 *             +-----------------------+
 *    start    |   pause               v
 * o---------->o---------->o---------->o
 *             ^           |    stop
 *             +-----------+
 *                 resume
 *             
 * Disclaimer: There's probably some kinda interface out there that does this
 * but I couldn't find anything simple like this.  This will also work with 
 * the ConcurrentComponentManager which manages access to a component.
 * 
 * @see ConcurrentComponentManager
 * 
 * @author elandau
 *
 */
public interface Component {
    /**
     * Start the component.  Spins up any thread pools, etc.
     */
    void start() throws Exception;
    
    /**
     * Stop the component.  Shuts down any thread pools.
     * @throws Exception
     */
    void stop() throws Exception;
    
    /**
     * Pause the component so that all threads and resources are still running
     * but not accepting commands
     * @throws Exception
     */
    void pause() throws Exception;
    
    /**
     * Resume a paused pool
     * @throws Exception
     */
    void resume() throws Exception;
}
