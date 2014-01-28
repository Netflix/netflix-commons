package com.netflix.eventbus.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.eventbus.spi.CatchAllSubscriber;
import com.netflix.eventbus.spi.EventBus;
import com.netflix.eventbus.spi.EventCreator;
import com.netflix.eventbus.spi.EventFilter;
import com.netflix.eventbus.spi.InvalidSubscriberException;
import com.netflix.eventbus.spi.Subscribe;
import com.netflix.eventbus.spi.SubscriberConfigProvider;
import com.netflix.eventbus.spi.SubscriberInfo;
import com.netflix.eventbus.utils.EventBusUtils;
import com.netflix.servo.monitor.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An implementation of {@link EventBus}. This implementation is based on the google eventbus
 * {@link com.google.common.eventbus.EventBus} but the absence of descent extension points forces us to inspire but
 * not extend that implementation. The critical parts that drove us towards this approach are:
 * <ul>
 <li>Inability to add a filter for publisher/subscriber. We would need to copy part of code to do this.</li>
 <li>Inability to easily create custom handler wrappers for our style of async dispatch.</li>
 </ul>
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
public class EventBusImpl implements EventBus {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBusImpl.class);

    static final DynamicIntProperty STATS_COLLECTION_DURATION_MILLIS =
            DynamicPropertyFactory.getInstance().getIntProperty("eventbus.stats.collection.duration.millis", 60*1000);

    /**
     * Event type VS consumers map. Any event for which consumers are required, must query this collection for all the
     * interfaces & classes the event implements/extends, directly or indirectly, typically by calling
     * {@link EventBusImpl#getAllTypesForAnEvent(Object)}
     */
    private final SetMultimap<Class<?>, EventConsumer> consumersByEventType =
            Multimaps.newSetMultimap(new ConcurrentHashMap<Class<?>, Collection<EventConsumer>>(),
                    new Supplier<Set<EventConsumer>>() {
                        @Override
                        public Set<EventConsumer> get() {
                            return new CopyOnWriteArraySet<EventConsumer>();
                        }
                    });

    /**
     * This is an index to aid in unregister of subscribers. This holds the mapping between, the class of the subscriber
     * and the list of consumers it is registered with. During unregister we get a list of consumers for that class
     * and then for every consumer get the event class it listens for and then delete the same from
     * {@link EventBusImpl#consumersByEventType}
     */
    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<EventConsumer>> consumersBySubscriberClass =
            new ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<EventConsumer>>();

    /**
     * Filters attached to an event type.
     */
    private final SetMultimap<Class<?>, EventFilter> eventTypeVsFilters =
            Multimaps.newSetMultimap(new ConcurrentHashMap<Class<?>, Collection<EventFilter>>(),
                    new Supplier<Set<EventFilter>>() {
                        @Override
                        public Set<EventFilter> get() {
                            return new CopyOnWriteArraySet<EventFilter>();
                        }
                    });

    /**
     * Cache of the class hierarchy for an event type. This optimizes multiple publishing of the same event type which
     * typically will be the case.
     */
    private static LoadingCache<Class<?>, Set<Class<?>>> eventHierarchyCache =
            CacheBuilder.newBuilder()
                        .weakKeys()
                        .build(new CacheLoader<Class<?>, Set<Class<?>>>() {
                            @Override
                            public Set<Class<?>> load(Class<?> concreteClass) throws Exception {
                                List<Class<?>> parents = Lists.newLinkedList();
                                Set<Class<?>> classes = Sets.newHashSet();

                                parents.add(concreteClass);

                                while (!parents.isEmpty()) {
                                    Class<?> clazz = parents.remove(0);
                                    classes.add(clazz);

                                    Class<?> parent = clazz.getSuperclass();
                                    if (parent != null && !parent.equals(Object.class)) { // Do not allow subs on java.lang.Object
                                        parents.add(parent);
                                    }

                                    Collections.addAll(parents, clazz.getInterfaces());
                                }

                                return classes;
                            }
                        });

    private ConsumerQueueSupplier consumerQueueSupplier = new DefaultConsumerQueueSupplier();

    private EventBusStats stats = new EventBusStats(STATS_COLLECTION_DURATION_MILLIS.get());

    private EventConsumer catchAllSubscriber;
    private volatile CatchAllSubscriber catchAllSubInstance;

    public EventBusImpl() {
        
    }

    @Override
    public void publish(Object event) {
        Stopwatch start = stats.publishStats.start();
        try {
            if (!applyEventLevelFilters(event)) {
                return;
            }

            Set<Class<?>> allTypesForAnEvent = getAllTypesForAnEvent(event);
            for (Class<?> eventType : allTypesForAnEvent) {
                Set<EventConsumer> eventConsumers = consumersByEventType.get(eventType);
                for (EventConsumer eventConsumer : eventConsumers) {
                    eventConsumer.enqueue(event);
                }
            }
            if (null != catchAllSubInstance && catchAllSubInstance.isEnabled()) {
                catchAllSubscriber.enqueue(event);
            }
        } catch (Throwable th) {
            LOGGER.error("Error occured while publishing event. Swallowing the error to avoid publisher from failing.", th);
            stats.publishErrors.increment();
        } finally {
            start.stop();
        }
    }

    @Override
    public void publishIffNotDead(EventCreator creator, Class<?>... eventTypes) {
        Stopwatch start = stats.conditionalPublishStats.start();
        try {
            Map<Class<?>, Set<EventConsumer>> interestedConsumersByType = new HashMap<Class<?>, Set<EventConsumer>>();
            for (Class<?> eventType : eventTypes) {
                for (Class<?> anEventSubType : getAllTypesForAnEventType(eventType)) {
                    Set<EventConsumer> eventConsumers = consumersByEventType.get(anEventSubType);
                    if (!eventConsumers.isEmpty()) {
                        /*
                        * Since any change in the underlying consumers get reflected in this set, we get the benefit of any changes
                        * to the consumers after this check being reflected when we invoke these consumers.
                        * We add the evenType to the map and not the subType as the event creator is only aware of the high
                        * level types & not the entire hierarchy.
                        */
                        interestedConsumersByType.put(eventType, eventConsumers);
                    }
                }
            }

            if (interestedConsumersByType.isEmpty()) {
                LOGGER.debug(String.format("Skipping publishing of events types %s as there are no interested listeners.",
                                           Arrays.toString(eventTypes)));
                return;
            }

            List events = creator.createEvent(interestedConsumersByType.keySet());
            if (null == events) {
                LOGGER.debug(String.format("No events created by event creator for event types %s",
                        interestedConsumersByType.keySet()));
                return;
            }

            for (Object event : events) {
                if (!applyEventLevelFilters(event)) {
                    continue;
                }
                Set<EventConsumer> eventConsumers = interestedConsumersByType.get(event.getClass());
                for (EventConsumer eventConsumer : eventConsumers) {
                    eventConsumer.enqueue(event);
                }
            }
        } catch (Throwable th) {
            LOGGER.error("Error occured while publishing event. Swallowing the error to avoid publisher from failing.", th);
            stats.conditionalPublishErrors.increment();
        } finally {
            start.stop();
        }
    }

    @Override
    public void registerSubscriber(@Nullable EventFilter filter, Object subscriber) throws InvalidSubscriberException {
        List<EventConsumer> allConsumersForThisSubscriber = new ArrayList<EventConsumer>();
        List<Method> subscriberMethods = findSubscriberMethods(subscriber);
        for (Method subscriberMethod : subscriberMethods) {
            Class<?> targetEventType = EventBusUtils.getInterestedEventType(subscriber, subscriberMethod);
            EventConsumer consumer =
                    new EventConsumer(subscriberMethod, subscriber, filter, targetEventType, consumerQueueSupplier);
            allConsumersForThisSubscriber.add(consumer);
            consumersByEventType.put(targetEventType, consumer);
        }

        CopyOnWriteArrayList<EventConsumer> existingConsumers =
                consumersBySubscriberClass.putIfAbsent(subscriber.getClass(),
                                                       new CopyOnWriteArrayList<EventConsumer>((allConsumersForThisSubscriber)));
        if (null != existingConsumers) {
            existingConsumers.addAll(allConsumersForThisSubscriber);
        } else {
            LOGGER.info(String.format("Registered a new subscriber: %s with filter: %s", subscriber, filter));
        }
    }

    @Override
    public void registerSubscriber(Object subscriber) throws InvalidSubscriberException {
        registerSubscriber(null, subscriber);
    }

    @Override
    public synchronized boolean enableCatchAllSubscriber(BlockingQueue catchAllSink) {
        if (null == catchAllSubscriber) {
            catchAllSubInstance = new CatchAllSubscriber();
            List<Method> subscriberMethods;
            try {
                subscriberMethods = findSubscriberMethods(catchAllSubInstance);
                if (!subscriberMethods.isEmpty()) {
                    Method method = subscriberMethods.get(0);
                    catchAllSubscriber = new EventConsumer(method, catchAllSubInstance, null, Object.class, consumerQueueSupplier);
                }
            } catch (InvalidSubscriberException e) {
                // We know it can not happen as the subscriber is valid.
                LOGGER.error("Catch all subscriber invalid!", e);
                return false;
            }
        }
        return catchAllSubInstance.enable(catchAllSink);
    }

    @Override
    public synchronized void disableCatchAllSubscriber() {
        if (null != catchAllSubInstance) {
            catchAllSubInstance.disable();
        } else {
            LOGGER.info("Catch all subscriber is not enabled, disable call ignored.");
        }
    }

    @Override
    public Set<Object> unregisterSubscriber(Class<?> subscriberClass) {
        LOGGER.info("Unregistring subscriber class: " + subscriberClass);
        Set<Object> toReturn = new HashSet<Object>();
        CopyOnWriteArrayList<EventConsumer> eventConsumers = consumersBySubscriberClass.remove(subscriberClass);
        if (null != eventConsumers) {
            for (EventConsumer eventConsumer : eventConsumers) {
                eventConsumer.shutdown();
                Class<?> targetEventClass = eventConsumer.getTargetEventClass();
                consumersByEventType.remove(targetEventClass, eventConsumer);
                toReturn.add(eventConsumer.getContainerInstance());
            }
            LOGGER.info(String.format("Subscriber: %s successfully unregistered", subscriberClass));
        } else {
            LOGGER.info(String.format("Subscriber: %s is not registered (or already removed). Ignoring unregister.",
                    subscriberClass));
        }
        return toReturn;
    }

    @Override
    public boolean unregisterSubscriber(Object subscriber) {
        LOGGER.info("Unregistring subscriber instance: " + subscriber);
        Class subscriberClass = subscriber.getClass();
        CopyOnWriteArrayList<EventConsumer> eventConsumers = consumersBySubscriberClass.get(subscriberClass);
        boolean unregistered = false;
        EventConsumer toRemove = null;
        if (null != eventConsumers) {
            for (EventConsumer eventConsumer : eventConsumers) {
                if (eventConsumer.getContainerInstance() == subscriber) {
                    toRemove = eventConsumer;
                }
            }

            if (null != toRemove && eventConsumers.remove(toRemove)) {
                toRemove.shutdown();
                Class<?> targetEventClass = toRemove.getTargetEventClass();
                consumersByEventType.remove(targetEventClass, toRemove);
                unregistered = true;
            }
        }

        if (unregistered) {
            LOGGER.info(String.format("Subscriber instance: %s successfully unregistered", subscriber));
        } else {
            LOGGER.info(String.format("Subscriber instance: %s is not registered (or already removed). Ignoring unregister.", subscriber));
        }

        return unregistered;
    }

    @Override
    public void addFilterForSubscriber(EventFilter filter, SubscriberInfo subscriberInfo) {
        String callDescription = "add filter";
        EventConsumer consumerInAction = findEventConsumerForSubscriberMethod(subscriberInfo, callDescription);
        if (null != consumerInAction) {
            consumerInAction.addFilters(filter);
            LOGGER.info(String.format("Added a new filter %s for subscriber method %s", filter,
                                      subscriberInfo.getSubscriberMethod().toGenericString()));
        }
    }

    @Override
    public void removeFiltersForSubscriber(SubscriberInfo subscriberInfo, EventFilter... filters) {
        String callDescription = "remove filter";
        EventConsumer consumerInAction = findEventConsumerForSubscriberMethod(subscriberInfo, callDescription);
        if (null != consumerInAction) {
            consumerInAction.removeFilters(filters);
            LOGGER.info(String.format("Removed filters %s for subscriber method %s", Arrays.toString(filters),
                    subscriberInfo.getSubscriberMethod().toGenericString()));
        }
    }

    @Override
    public void clearFiltersForSubscriber(SubscriberInfo subscriberInfo) {
        String callDescription = "add filter";
        EventConsumer consumerInAction = findEventConsumerForSubscriberMethod(subscriberInfo, callDescription);
        if (null != consumerInAction) {
            consumerInAction.clearFilters();
            LOGGER.info(
                    String.format("Removed ALL filters for subscriber method %s", subscriberInfo.getSubscriberMethod().toGenericString()));
        }
    }

    @Override
    public void addFilterForEvent(EventFilter filter, Class<?> eventClass) {
        boolean modified = eventTypeVsFilters.put(eventClass, filter);
        if (modified) {
            LOGGER.info(String.format("Added a new filter %s for the event type: %s", filter, eventClass));
        } else {
            LOGGER.info(String.format("Filter %s already exists for the event type: %s", filter, eventClass));
        }
    }

    @Override
    public void removeFiltersForEvent(Class<?> eventClass, EventFilter... filters) {
        if (null == filters || filters.length == 0) {
            return;
        }
        Set<EventFilter> eventFilters = eventTypeVsFilters.get(eventClass);
        boolean modified = eventFilters.removeAll(Arrays.asList(filters));
        if (modified) {
            LOGGER.info(String.format("Removed filters %s for event type %s", Arrays.toString(filters), eventClass));
        } else {
            LOGGER.info(String.format("None of the filters %s exists for event type %s. Ignoring remove.", Arrays.toString(
                    filters), eventClass));
        }
    }

    @Override
    public void clearFiltersForEvent(Class<?> eventClass) {
        eventTypeVsFilters.removeAll(eventClass);
        LOGGER.info(String.format("Removed ALL filters for event type %s", eventClass));
    }

    @Override
    public Set<SubscriberInfo> getAllSubscribers() {
        Set<SubscriberInfo> toReturn = new HashSet<SubscriberInfo>();
        for (CopyOnWriteArrayList<EventConsumer> eventConsumers : consumersBySubscriberClass.values()) {
            for (EventConsumer eventConsumer : eventConsumers) {
                toReturn.add(new SubscriberInfo(eventConsumer.getDelegateSubscriber(), eventConsumer.getContainerInstance()));
            }

        }
        return Collections.unmodifiableSet(toReturn);
    }

    @Override
    public Set<SubscriberInfo> getAllSubscribersForAnEvent(Class<?> eventType) {
        Set<EventConsumer> eventConsumers = consumersByEventType.get(eventType);
        if (null == eventConsumers || eventConsumers.isEmpty()) {
            return Collections.emptySet();
        }
        Set<SubscriberInfo> toReturn = new HashSet<SubscriberInfo>(eventConsumers.size());
        for (EventConsumer eventConsumer : eventConsumers) {
            toReturn.add(new SubscriberInfo(eventConsumer.getDelegateSubscriber(), eventConsumer.getContainerInstance()));
        }
        return Collections.unmodifiableSet(toReturn);
    }

    @Override
    public Set<EventFilter> getFilterForASubscriber(SubscriberInfo subscriberInfo) {
        String callDescription = "get filter";
        EventConsumer consumerInAction = findEventConsumerForSubscriberMethod(subscriberInfo, callDescription);
        if (null == consumerInAction) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(consumerInAction.getAttachedFilters());
    }

    @Override
    public Set<EventFilter> getFiltersForAnEvent(Class<?> eventType) {
        Set<EventFilter> eventFilters = eventTypeVsFilters.get(eventType);
        return Collections.unmodifiableSet(eventFilters);
    }

    @Override
    public Set<Class<?>> getAllRegisteredEventTypes() {
        Set<Class<?>> eventTypesWithConsumers = consumersByEventType.keySet();
        Set<Class<?>> eventTypesWithFilters = eventTypeVsFilters.keySet();
        return Sets.union(eventTypesWithConsumers, eventTypesWithFilters);
    }

    public synchronized void shutdown() {
        Collection<EventConsumer> consumers = consumersByEventType.values();
        for (EventConsumer consumer : consumers) {
            consumer.shutdown();
        }
        // Clearing the data as the event bus instance *may* be stored somewhere & not GC'ed
        consumersByEventType.clear();
        consumersBySubscriberClass.clear();
        eventTypeVsFilters.clear();
    }

    @VisibleForTesting
    void setConsumerQueueSupplier(ConsumerQueueSupplier consumerQueueSupplier) {
        this.consumerQueueSupplier = consumerQueueSupplier;
    }

    @VisibleForTesting
    Set<EventConsumer> getEventConsumer(Class eventClass) {
        return consumersByEventType.get(eventClass);
    }

    private boolean applyEventLevelFilters(Object event) {
        return EventBusUtils.applyFilters(event, eventTypeVsFilters.get(event.getClass()), stats.filterStats,
                                          " publisher ", LOGGER);
    }

    /**
     * Finds all the subscriber methods defined in the passed subscriber class. Also, validates whether the subscriber
     * methods adhere to the rules of the game using {@link SubscriberValidator}
     *
     * @param subscriber Subscriber instance for which the subscriber methods are to be found.
     *
     * @return List of valid subscriber methods.
     *
     * @throws InvalidSubscriberException If any one of the method is invalid.
     */
    private List<Method> findSubscriberMethods(Object subscriber) throws InvalidSubscriberException {
        List<Method> subscriberMethods = new ArrayList<Method>();
        Set<Method> allMethods = new HashSet<Method>();
        Method[] methods = subscriber.getClass().getMethods();
        allMethods.addAll(Arrays.asList(methods));
        // This enables declaring even private methods in a consumer. We will try to enable access, failure of which
        // will remove this method from the methods list.
        Method[] allDeclaredMethods = subscriber.getClass().getDeclaredMethods();
        allMethods.addAll(Arrays.asList(allDeclaredMethods));
        for (Method method : allMethods) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                try {
                    method.setAccessible(true);
                    subscriberMethods.add(method);
                } catch (SecurityException e) {
                    LOGGER.error("A subscriber method: " + method.toGenericString() +
                                 " is not a public method and the security settings does not allow accessing non-public"
                                 +
                                 " methods via reflection. This subscriber method will not be registered.", e);
                }
            }
        }
        Map<Method, String> errors = SubscriberValidator.validate(subscriber, subscriberMethods);
        if (!errors.isEmpty()) {
            throw new InvalidSubscriberException(subscriber.getClass(), errors);
        }
        return subscriberMethods;
    }

    /**
     * Finds the {@link EventConsumer} instance registered with this event bus, for the passed <code>subscriberMethod</code>
     *
     * @param subscriberInfo Subscriber information.
     * @param callDescription Description of the original call for which this method is invoked. This is used for
     *                        logging.
     *
     * @return The {@link EventConsumer} instance. <code>null</code> if none found.
     */
    private EventConsumer findEventConsumerForSubscriberMethod(SubscriberInfo subscriberInfo, String callDescription) {
        Method subscriberMethod = subscriberInfo.getSubscriberMethod();
        Subscribe subscribeAnnotation = subscriberMethod.getAnnotation(Subscribe.class);
        if (null == subscribeAnnotation) {
            LOGGER.error(String.format("The subscriber method: %s is not annotated with @Subscribe. Ignoring %s call.",
                    subscriberMethod, callDescription));
            return null;
        }
        CopyOnWriteArrayList<EventConsumer> eventConsumers = consumersBySubscriberClass.get(
                subscriberMethod.getDeclaringClass());
        if (null != eventConsumers) {
            // Here what we are dealing with is the subscriber methods in a SINGLE class, so O(n) will suffice.
            for (EventConsumer eventConsumer : eventConsumers) {
                if (isTheSameSubscriber(subscriberInfo, eventConsumer)) {
                    return eventConsumer;
                }
            }
        }
        LOGGER.info(String.format("Subscriber: %s is not registered (or already removed). Ignoring %s call.",
                                  subscriberMethod, callDescription));
        return null;
    }

    private static Set<Class<?>> getAllTypesForAnEvent(Object event) {
        try {
            return eventHierarchyCache.get(event.getClass());
        } catch (ExecutionException e) {
            throw Throwables.propagate(e.getCause());
        }
    }

    private static Set<Class<?>> getAllTypesForAnEventType(Class eventType) {
        try {
            return eventHierarchyCache.get(eventType);
        } catch (ExecutionException e) {
            throw Throwables.propagate(e.getCause());
        }
    }

    private boolean isTheSameSubscriber(SubscriberInfo subscriberInfo, EventConsumer eventConsumer) {
        return eventConsumer.getDelegateSubscriber().equals(subscriberInfo.getSubscriberMethod())
               && eventConsumer.getContainerInstance() == subscriberInfo.getSubscriberInstance();
    }

    /**
     * Apart from testing, there isn't really a reason to override this. <p/>
     * Our batching strategy is completely built on top of the queue implementation so care should be taken while using
     * a custom supplier so that it provides all the correct queues for all batching strategies.
     */
    @VisibleForTesting
    interface ConsumerQueueSupplier {

        /**
         * Creates a new instance of the queue based on the passed <code>subscribe</code> annotation.
         *
         * @param subscriberMethod The subscriber method for the consumer.
         * @param subscriberConfig Subscriber configuration.
         * @param queueSizeCounter A counter that holds the current queue size.
         *  @return The queue instance for the passed parameters.
         */
        ConsumerQueue get(Method subscriberMethod, SubscriberConfigProvider.SubscriberConfig subscriberConfig,
                          AtomicLong queueSizeCounter);

        /**
         * This is a stripped down version of {@link BlockingQueue} to only expose methods that are required by the
         * {@link EventConsumer}. The intention of stripping down the functionality is to have more control over the
         * queue implementation which in turn completely supplies the batching strategy. If the consumer starts using
         * other methods of the {@link BlockingQueue} our batching may completely break. Using {@link BlockingQueue} will
         * mean implementing methods that we never use.
         */
        interface ConsumerQueue {

            /**
             * Offer an event to the queue without blocking. If the queue is full, this returns <code>false</code>, if
             * there is space the event is added to the queue.
             *
             * @param event Event to add to the queue.
             *
             * @return <code>true</code>  if the event was added, <code>false</code> otherwise.
             */
            boolean offer(Object event);

            /**
             * Removes an element from the queue. This method does not block and returns <code>null</code>  if none is
             * available.
             *
             * @return Event if available. <code>null</code> otherwise.
             */
            Object nonBlockingTake();

            /**
             * Removes an element from the queue. This method blocks till an event is available.
             *
             * @return Next event in the queue.
             *
             * @throws InterruptedException If the wait gets interrupted.
             */
            Object blockingTake() throws InterruptedException;

            /**
             * Clears the queue and disposes the events.
             */
            void clear();
        }
    }
}
