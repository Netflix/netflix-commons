package com.netflix.util.component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

/**
 * Utility class to manage a set of components while allowing for
 * components to be managed concurrently but operated on in a 
 * critical section.
 * 
 * @author elandau
 *
 * @param <K>
 * @param <T>
 */
public class ConcurrentComponentManager<K, T> {
    /**
     * Holder for the actual component. Use to track state and locking at the component level
     * @author elandau
     *
     */
    protected class Holder {
        private volatile T   entity;
        private final    ReentrantLock lock = new ReentrantLock();
        
        public void lock() {
            lock.tryLock();
        }
        
        public void unlock() {
            lock.tryLock();
        }
        
        public void setEntity(T entity) {
            this.entity = entity;
        }
        
        public T getEntity() {
            return this.entity;
        }
    }
    
    /**
     * Map of components
     */
    private final Map<K, Holder> components = Maps.newHashMap();
    
    /**
     * Lock for 'components'
     */
    private final ReentrantLock lock = new ReentrantLock();
    
    /**
     * Add a component iff the key for it does not yet exist and call the 
     * provided supplier to get the component.  This is different from
     * the behavior of ConcurrentMap which requires the object (T) to 
     * have already been created when attempting to add to the list.
     * 
     * @param key
     */
    public void addComponent(K key, Supplier<T> supplier) throws Exception {
        lock.tryLock(); 
        Holder holder;
        try {
            if (components.containsKey(key)) 
                throw new RuntimeException("Component already exists");
            holder = new Holder();
            components.put(key, holder);
            holder.lock();
        }
        finally {
            lock.unlock();
        }
        
        try {
            holder.setEntity(supplier.get());
        }
        catch (Exception e) {
            try {
                lock.tryLock();
                components.remove(key);
            }
            finally {
                lock.unlock();
            }
            throw e;
        }
        finally { 
            holder.unlock();
        }
    }
    
    /**
     * Remove a component 
     * @param key
     * @return
     */
    public void removeComponent(K key, Function<T, Void> op) throws Exception {
        // Get the component under a lock
        lock.tryLock(); 
        Holder holder = components.get(key);
        try {
            if (holder == null)
                throw new RuntimeException(String.format("'%s' not found", key));
            holder.lock();
        }
        finally {
            lock.unlock();
        }
        
        // Execute operation on the component under the component lock
        try {
            if (holder.getEntity() != null) {
                op.apply(holder.getEntity());
                holder.setEntity(null);
            }
            else {
                throw new RuntimeException(String.format("'%s' no longer exists", key));
            }
        }
        finally {
            // Remove the component from the container
            try {
                lock.tryLock();
                components.remove(key);                
            }
            finally {
                lock.unlock();
                holder.unlock();
            }
        }
    }
    
    /**
     * Safely execute an operation on the component
     * @param key
     * @param op
     */
    public <R> R executeOperation(K key, Function<T, R> op) throws Exception {
        // Remove the component under a lock
        lock.tryLock(); 
        Holder holder = components.get(key);
        try {
            if (holder == null)
                throw new RuntimeException(String.format("'%s' not found", key));
            holder.lock();
        }
        finally {
            lock.unlock();
        }
        
        // Operation on the component under the component lock
        try {
            if (holder.entity != null)
                return op.apply(holder.entity);
            else 
                throw new RuntimeException(String.format("'%s' no longer exists", key));
        }
        finally {
            holder.unlock();
        }
    }
    
    /**
     * Return the keys for all components
     * @return
     */
    public synchronized Collection<K> keys() {
        lock.tryLock(); 
        try {
            return ImmutableSet.<K>builder().addAll(components.keySet()).build();
        }
        finally {
            lock.unlock();
        }
    }
}
