/**
 * Eventbus state, primarily event filters, can be persisted into a persistence store and can be altered externally. <br/>
 * This provides a way to define the persistence scheme as well as a daemon that will poll the persistent store for any
 * change from the last retrieved value. <br/>
 * The primary use-case for this model will be providing actions on a cluster of machines which if modelled as a
 * scatter-gather call will be prone to inconsistencies due to machine unavailability during network partitions or
 * general failure to respond to external requests. <br/>
 * Any instance participating in a cluster wide update will only be available for local changes in lieu of disconnecting
 * from the cluster updates. In no scenario, the instance will be actively participating in changes from a persistence
 * store as well as directly on an instance. Of course, changes outside the boundaries of event bus can still happen on
 * an instance (via dynamic registration at runtime) with the understanding that it may cause inconsistencies. <br/>
 *
 *
 */
package com.netflix.eventbus.persistence;
