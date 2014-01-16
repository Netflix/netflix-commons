package com.netflix.eventbus.persistence;

/**
 * A persistence handler for eventbus.
 *
 * @author Nitesh Kant
 */
public interface PersistenceHandler {

    void start();

    void stop();
}
