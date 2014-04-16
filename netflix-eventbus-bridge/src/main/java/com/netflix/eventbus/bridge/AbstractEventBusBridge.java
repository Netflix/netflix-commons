package com.netflix.eventbus.bridge;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.Inject;
import com.netflix.eventbus.spi.DynamicSubscriber;
import com.netflix.eventbus.spi.EventBus;
import com.netflix.eventbus.spi.EventFilter;
import com.netflix.eventbus.spi.Subscribe;

/**
 * Implements basic functionality of registering for the event bus and
 * forwarding events to another messaging API.
 * 
 * @author elandau
 *
 */
public abstract class AbstractEventBusBridge implements EventBusBridge {
    public static final Boolean DEFAULT_AUTO_START = true;
    public static final Supplier<EventBusBridgeStats> DEFAULT_STATS_SUPPLIER = new Supplier<EventBusBridgeStats>() {
        @Override
        public EventBusBridgeStats get() {
            return new SimpleEventBusBridgeStats();
        }
    };
    
    /**
     * Base builder with support for fluent subsclasses
     * 
     * Note that the subclass's Build method MUST call validate().
     * 
     * @param <T>   The subclasses's Builder.
     */
    public static abstract class Builder<T extends Builder<T>> {
        protected EventBus eventBus;
        protected Class<?> eventType;
        protected Supplier<EventBusBridgeStats> statsSupplier = DEFAULT_STATS_SUPPLIER;
        protected boolean autoStart = DEFAULT_AUTO_START;
        protected EventFilter filter;
        
        /**
         * The event bus to use
         * @param eventBus
         */
        @Inject
        public T withEventBus(EventBus eventBus) {
            this.eventBus = eventBus;
            return self();
        }
        
        /**
         * Specify the event type to listen to.  This can be a primitive, Pojo, or
         * Annotatable. 
         * @param eventType
         */
        public T withEventType(Class<?> eventType) {
            this.eventType = eventType;
            return self();
        }
        
        /**
         * Predicate used to filter which events may be sent to the sink
         * @param filter
         */
        public T withFilter(EventFilter filter) {
            this.filter = filter;
            return self();
        }
        
        /**
         * Externally provided stats supplier
         * @param supplier
         */
        public T withStatsSupplier(Supplier<EventBusBridgeStats> supplier) {
            if (supplier != null)
                this.statsSupplier = supplier;
            return self();
        }

        /**
         * Indicate whether the bridge should begin consuming messages immediately
         * after being constructed, otherwise events will not be consumed until
         * resume() is called.
         * @param autoStart
         * @return
         */
        public T withAutoStart(Boolean autoStart) {
            this.autoStart = autoStart;
            return self();
        }
        
        /**
         * Externally provided specific instance of stats object
         * @param stats
         */
        public T withStats(EventBusBridgeStats stats) {
            if (stats != null)
                this.statsSupplier = Suppliers.ofInstance(stats);
            return self();
        }
        
        protected void validate() throws Exception {
            Preconditions.checkNotNull(eventType,  "Must specify an event type");
            Preconditions.checkNotNull(eventBus,   "Must specify an event bus");
        }
        
        /**
         * Trick to allow for fluent API using the base Builder and the subclass's
         * builder.  The subclass must implement self() as 'return this;'.
         * @return
         */
        protected abstract T self();
    }
    
    protected final EventBus            eventBus;
    protected final EventBusBridgeStats stats;
    protected final Class<?>            eventType;
    protected final Object              subscriber;
    protected final EventFilter         filter;
    protected volatile Boolean          paused = false;
    
    protected AbstractEventBusBridge(Builder<?> init) throws Exception {
        this.eventBus  = init.eventBus;
        this.stats     = init.statsSupplier.get();
        this.eventType = init.eventType;
        this.paused    = !init.autoStart;
        this.filter    = init.filter;
        
        this.subscriber = new DynamicSubscriber() {
            @Override
            public Class<?> getEventType() {
                return eventType;
            }

            @Subscribe
            public void consume(Object obj) {
                try {
                    if (!paused) {
                        sendEvent(obj);
                        stats.incConsumeCount();
                    }
                }
                catch (Exception e) {
                    stats.incConsumeErrorCount(e);
                }
            }
        };
    }
    
    @PostConstruct
    final public void init() throws Exception  {
        preInit();
        if (!this.isPaused()) {
            paused = true;
            resume();
        }
    }
    
    @PreDestroy
    final public void shutdown() throws Exception {
        pause();
        postShutdown();
    }
    
    /**
     * Template method for sending an event
     * @param event
     * @throws Exception
     * @deprecated Use {@link onNextEvent} instead
     */
    @Deprecated
    protected abstract void sendEvent(Object event) throws Exception;
    
    protected void onNextEvent(Object event) throws Exception {
        sendEvent(event);
    }
    
    /**
     * Template method giving the subclass a chance to initialize any connection
     * as init time.
     * @throws Exception
     */
    protected void preInit() throws Exception {
    }
    
    /**
     * Template method giving the subclass a chance to do final cleanup after
     * the bridge is terminated
     * @throws Exception
     */
    protected void postShutdown() throws Exception {
    }
    
    /**
     * Template method giving the subclass a chance to perform any necessary 
     * steps prior to resuming consumption of messages.  Note that preResume()
     * is called immediately after preInit() during the first initialization 
     * phase.   This is a good place to resume any threads previous
     * 
     * This call is thread safe.
     * 
     * @throws Exception
     */
    protected void preResume() throws Exception {
    }
    
    /**
     * Template method giving the subclass a chance to perform any necessary
     * steps immediately after pausing consumption of messages.  This is a 
     * good place to temporarily suspend any threads specific to the 
     * bridge implementation.
     * 
     * This call is thread safe.
     * 
     * @throws Exception
     */
    protected void postPause() throws Exception {
    }
    
    @Override
    final public synchronized void pause() throws Exception {
        if (paused == false) {
            paused = true;
            this.eventBus.unregisterSubscriber(subscriber);
            postPause();
        }
    }
    
    @Override
    final public synchronized void resume() throws Exception {
        if (paused == true) {
            preResume();
            if (filter != null)
                this.eventBus.registerSubscriber(filter, subscriber);
            else 
                this.eventBus.registerSubscriber(subscriber);        
            paused = false;
        }
    }
    
    public boolean isPaused() {
        return paused;
    }
    
    public long getConsumeErrorCount() {
        return stats.getConsumeErrorCount();
    }
    
    public long getConsumeCount() {
        return stats.getConsumeCount();
    }
    
    public Exception getLastConsumeException() {
        return stats.getLastConsumeException();
    }
    
    public EventBusBridgeStats getStats() {
        return new ImmutableEventBusBridgeStats(stats);
    }
}
