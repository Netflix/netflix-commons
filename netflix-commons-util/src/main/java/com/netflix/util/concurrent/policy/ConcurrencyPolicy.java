package com.netflix.util.concurrent.policy;

import java.util.concurrent.ExecutorService;

/**
 * Policy for creating or getting an ExecutorService.
 * 
 * @author elandau
 *
 */
public interface ConcurrencyPolicy {
    /**
     * @return Return a new or shared executor service to be used by a policy
     *         driven component
     *         
     * @param name  Used to name the threads.  Note that this may be ignored for shared naming.
     * @return
     */
    public ExecutorService getExecutorService(String name);
}
