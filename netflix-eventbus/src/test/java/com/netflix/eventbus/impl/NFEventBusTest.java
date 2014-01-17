package com.netflix.eventbus.impl;

import com.google.common.base.Predicate;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.eventbus.filter.lang.infix.InfixEventFilter;
import com.netflix.eventbus.filter.lang.infix.InfixFilterLanguageSupport;
import com.netflix.eventbus.spi.DynamicSubscriber;
import com.netflix.eventbus.spi.EventCreator;
import com.netflix.eventbus.spi.EventFilter;
import com.netflix.eventbus.spi.InvalidSubscriberException;
import com.netflix.eventbus.spi.Subscribe;
import com.netflix.eventbus.spi.SubscriberConfigProvider;
import com.netflix.eventbus.spi.SyncSubscribersGatekeeper;
import com.netflix.eventbus.test.AnonymousInnerClassConsumerSupplier;
import com.netflix.infix.MockAnnotatable;
import com.netflix.infix.NumericValuePredicate;
import com.netflix.infix.PathValueEventFilter;
import com.netflix.infix.Predicates;
import com.netflix.infix.StringValuePredicate;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NFEventBusTest {

    private EventBusImpl eventBus;

    @Before
	public void setUp() throws Exception {
        System.setProperty(SyncSubscribersGatekeeper.ALLOW_SYNC_SUBSCRIBERS, "false");
        eventBus = new EventBusImpl();
    }

	@After
	public void tearDown() throws Exception {
        if (null != eventBus) {
            eventBus.shutdown();
        }
    }

    @Test
    public void testAnonymousInnerClassConsumer() throws Exception {
        EventBusImpl bus = eventBus;
        final TestAwareQueue testAwareQueue = new TestAwareQueue();
        bus.setConsumerQueueSupplier(new EventBusImpl.ConsumerQueueSupplier() {
            @Override
            public ConsumerQueue get(Method subscriber, SubscriberConfigProvider.SubscriberConfig subscriberConfig, AtomicLong queueSizeCounter) {
                return testAwareQueue.setQueueSizeCounter(queueSizeCounter);
            }
        });

        bus.registerSubscriber(AnonymousInnerClassConsumerSupplier.getAnonymousInnerClassConsumer(Event.class));

        bus.publish(new Event("name", 1));

        assertEquals("Event not offered.", 1, testAwareQueue.offeredCount.get());
    }

    @Test
	public void testCatchAll() throws InvalidSubscriberException, InterruptedException {
        eventBus = new EventBusImpl();
        final TestAwareQueue testAwareQueue = new TestAwareQueue();
		eventBus.setConsumerQueueSupplier(new EventBusImpl.ConsumerQueueSupplier() {
            @Override
            public ConsumerQueue get(Method subscriber, SubscriberConfigProvider.SubscriberConfig subscriberConfig,
                                     AtomicLong queueSizeCounter) {
                return testAwareQueue.setQueueSizeCounter(queueSizeCounter);
            }
        });

        LinkedBlockingQueue q = new LinkedBlockingQueue();
		eventBus.enableCatchAllSubscriber(q);
		eventBus.publish(new Event("name", 1));
        assertEquals("No event enqueued to catch all subscriber.", 1, testAwareQueue.offeredCount.get());
        assertNotNull("No event enqueued to catch all subscriber sink under 1 second.", q.poll(1, TimeUnit.SECONDS));
        eventBus.disableCatchAllSubscriber();
        q.clear();
        testAwareQueue.offeredCount.set(0);

        eventBus.publish(new Event("name", 1));
        assertEquals("Event enqueued to catch all subscriber after disable.", 0, testAwareQueue.offeredCount.get());
        assertNull("Event enqueued to catch all subscriber sink under 1 second after disable.", q.poll(1, TimeUnit.SECONDS));
    }

	@Test
	public void testUnFilteredEventsWillBeDelivered() throws InvalidSubscriberException {
		EventBusImpl bus = eventBus;
        final TestAwareQueue testAwareQueue = new TestAwareQueue();
		bus.setConsumerQueueSupplier(new EventBusImpl.ConsumerQueueSupplier() {
            @Override
            public ConsumerQueue get(Method subscriber, SubscriberConfigProvider.SubscriberConfig subscriberConfig, AtomicLong queueSizeCounter) {
                return testAwareQueue.setQueueSizeCounter(queueSizeCounter);
            }
        });
		Predicate<Object> filter = Predicates.or(
			new PathValueEventFilter("name", new StringValuePredicate("name")),
			new PathValueEventFilter("id", new NumericValuePredicate(1, "="))
		);

		EventHandler handler = new EventHandler();
		bus.registerSubscriber(new InfixEventFilter(filter), handler);
		bus.publish(new Event("name", 1));

        checkIfEventOfferedAndDispatched(testAwareQueue, handler);
    }

    @Test
	public void testFilteredEventsWillNotBeDelivered() throws InvalidSubscriberException {
		EventBusImpl bus = eventBus;
        final TestAwareQueue testAwareQueue = new TestAwareQueue();
		bus.setConsumerQueueSupplier(new EventBusImpl.ConsumerQueueSupplier() {
            @Override
            public ConsumerQueue get(Method subscriber, SubscriberConfigProvider.SubscriberConfig subscriberConfig, AtomicLong queueSizeCounter) {
                return testAwareQueue.setQueueSizeCounter(queueSizeCounter);
            }
        });
		Predicate<Object> filter = Predicates.and(
                new PathValueEventFilter("name", new StringValuePredicate("name")),
                new PathValueEventFilter("id", new NumericValuePredicate(1, "="))
        );

		EventHandler handler = new EventHandler();
		bus.registerSubscriber(new InfixEventFilter(filter), handler);
		bus.publish(new Event("name", 2));

		assertEquals("Event not filtered.", 0, handler.counterMockAnnotatable.get());
    }

	@Test
	public void testNoPublishIfDead() throws InvalidSubscriberException {
		EventBusImpl bus = eventBus;
        TestAwareEventCreator creator = new TestAwareEventCreator();
        bus.publishIffNotDead(creator, Event.class);
		assertEquals("Event creation called even with no handlers", 0, creator.createEventCount.get());
    }

	@Test
	public void testPublishIfNotDeadNoFilter() throws InvalidSubscriberException {
		EventBusImpl bus = eventBus;
        final TestAwareQueue testAwareQueue = new TestAwareQueue();
        bus.setConsumerQueueSupplier(new EventBusImpl.ConsumerQueueSupplier() {

            @Override
            public ConsumerQueue get(Method subscriber, SubscriberConfigProvider.SubscriberConfig subscriberConfig, AtomicLong queueSizeCounter) {
                return testAwareQueue.setQueueSizeCounter(queueSizeCounter);
            }
        });

        EventHandler handler = new EventHandler();
        bus.registerSubscriber(handler);

        TestAwareEventCreator creator = new TestAwareEventCreator();
        bus.publishIffNotDead(creator, Event.class);
		assertEquals("Event creation not called even with handlers", 1, creator.createEventCount.get());
    }

	@Test
	public void testPublishIfNotDeadMultipleEventTypes() throws InvalidSubscriberException {
		EventBusImpl bus = eventBus;
        final TestAwareQueue testAwareQueue = new TestAwareQueue();
        bus.setConsumerQueueSupplier(new EventBusImpl.ConsumerQueueSupplier() {
            @Override
            public ConsumerQueue get(Method subscriber, SubscriberConfigProvider.SubscriberConfig subscriberConfig, AtomicLong queueSizeCounter) {
                return testAwareQueue.setQueueSizeCounter(queueSizeCounter);
            }
        });

        EventHandler handler = new EventHandler();
        bus.registerSubscriber(handler);

        TestAwareEventCreator creator = new TestAwareEventCreator(new Event("name", 1));
        bus.publishIffNotDead(creator, Event.class, Event2.class);

		assertEquals("Event creation not called even with handlers", 1, creator.createEventCount.get());
		assertFalse("Live event set contains unregistered handler's event",
                creator.lastLiveEventTypes.contains(Event2.class));
		assertFalse("Live event set contains event's superclass",
                creator.lastLiveEventTypes.contains(MockAnnotatable.class));
		assertTrue("Live event set does not contains live event type",
                creator.lastLiveEventTypes.contains(Event.class));

        checkIfEventOfferedAndDispatched(testAwareQueue, handler);

    }

	@Test
	public void testPublishIfNotDeadForAllHandlers() throws InvalidSubscriberException {
		EventBusImpl bus = eventBus;

        TestAwareConsumerQueueSupplier consumerQueueSupplier = new TestAwareConsumerQueueSupplier();

        bus.setConsumerQueueSupplier(consumerQueueSupplier);

        EventHandler handler = new EventHandler();
        bus.registerSubscriber(handler);
        TestAwareQueue handler1Q = consumerQueueSupplier.testAwareQueue;

        Event2Handler handler2 = new Event2Handler();
        bus.registerSubscriber(handler2);
        TestAwareQueue handler2Q = consumerQueueSupplier.testAwareQueue; // this last queue returned, so it works with this model

        TestAwareEventCreator creator = new TestAwareEventCreator(new Event("name", 1), new Event2("name2", 2));
        bus.publishIffNotDead(creator, Event.class, Event2.class);

		assertEquals("Event creation not called even with handlers", 1, creator.createEventCount.get());
		assertFalse("Live event set contains event's superclass", creator.lastLiveEventTypes.contains(MockAnnotatable.class));
        assertTrue("Live event set does not contains live event type", creator.lastLiveEventTypes.contains(Event2.class));
        assertTrue("Live event set does not contains live event type", creator.lastLiveEventTypes.contains(Event.class));

        checkIfEventOfferedAndDispatched(handler1Q, handler);
        checkIfEventOfferedAndDispatched(handler2Q, handler2);

    }

	@Test
	public void testPublishIfNotDeadForOneHandler() throws InvalidSubscriberException {
		EventBusImpl bus = eventBus;

        TestAwareConsumerQueueSupplier consumerQueueSupplier = new TestAwareConsumerQueueSupplier();

        bus.setConsumerQueueSupplier(consumerQueueSupplier);

        EventHandler handler = new EventHandler();
        bus.registerSubscriber(handler);
        TestAwareQueue handler1Q = consumerQueueSupplier.testAwareQueue;

        Event2Handler handler2 = new Event2Handler();
        bus.registerSubscriber(handler2);
        TestAwareQueue handler2Q = consumerQueueSupplier.testAwareQueue; // this last queue returned, so it works with this model

        TestAwareEventCreator creator = new TestAwareEventCreator(new Event("name", 1));
        bus.publishIffNotDead(creator, Event.class, Event2.class);

		assertEquals("Event creation not called even with handlers", 1, creator.createEventCount.get());
		assertFalse("Live event set contains event's superclass", creator.lastLiveEventTypes.contains(MockAnnotatable.class));
        assertTrue("Live event set does not contains live event type",
                creator.lastLiveEventTypes.contains(Event2.class));
        assertTrue("Live event set does not contains live event type", creator.lastLiveEventTypes.contains(Event.class));

        checkIfEventOfferedAndDispatched(handler1Q, handler);

        assertEquals("Handler received event which was not created!", 0, handler2Q.offeredCount.get());
    }

    @Test
    public void testMultipleSubsOfSameClass() throws Exception {
        EventBusImpl bus = eventBus;
        TestAwareConsumerQueueSupplier consumerQueueSupplier = new TestAwareConsumerQueueSupplier();
        bus.setConsumerQueueSupplier(consumerQueueSupplier);

        StatefulEventHandler handler1 = new StatefulEventHandler("con1");
        EventFilter filter = new InfixFilterLanguageSupport().convert("xpath(\"//name\")=\"con1\"");
        bus.registerSubscriber(filter, handler1);
        TestAwareQueue handler1Q = consumerQueueSupplier.testAwareQueue;

        StatefulEventHandler handler2 = new StatefulEventHandler("con2");
        filter = new InfixFilterLanguageSupport().convert("xpath(\"//name\")=\"con1\"");
        bus.registerSubscriber(filter, handler2);
        TestAwareQueue handler2Q = consumerQueueSupplier.testAwareQueue;

        StatefulEventHandler handler3 = new StatefulEventHandler("con3");
        filter = new InfixFilterLanguageSupport().convert("xpath(\"//name\")=\"con3\"");
        bus.registerSubscriber(filter, handler3);
        bus.publish(new Event("con1", 1));

        checkIfEventOfferedAndDispatched(handler1Q, handler1);
        checkIfEventOfferedAndDispatched(handler2Q, handler2);
        assertEquals("Event offered to handler3", 0, handler3.counterMockAnnotatable.get());
    }

    @Test
    public void testUnregisterSubClasss() throws Exception {
        EventBusImpl bus = eventBus;
        StatefulEventHandler sub1 = new StatefulEventHandler("1");
        StatefulEventHandler sub2 = new StatefulEventHandler("2");

        bus.registerSubscriber(sub1);
        bus.registerSubscriber(sub2);

        Set<Object> removedSubs = bus.unregisterSubscriber(StatefulEventHandler.class);

        Assert.assertTrue("No subscriber removed.", removedSubs != null && !removedSubs.isEmpty());
        Assert.assertEquals("All subscribers of the same class not removed.", 2, removedSubs.size());
    }

    @Test
    public void testUnregisterSubInstance() throws Exception {
        EventBusImpl bus = eventBus;
        StatefulEventHandler sub1 = new StatefulEventHandler("1");
        StatefulEventHandler sub2 = new StatefulEventHandler("2");

        bus.registerSubscriber(sub1);
        bus.registerSubscriber(sub2);

        boolean removed = bus.unregisterSubscriber(sub2);

        Assert.assertTrue("Subscriber instance not removed.", removed);

        bus.publishIffNotDead(new EventCreator() {
            @Override
            public List createEvent(Set<Class<?>> liveEventTypes) {
                return null;
            }
        });
    }

    @Test
    public void testSubOnObjectDisallowed() throws Exception {
        EventBusImpl bus = eventBus;
        try {
            bus.registerSubscriber(new Object() {
                @Subscribe
                @SuppressWarnings("unused")
                public void sub(Object king) {
                    //
                }
            });
            throw new AssertionError("Subscriber on java.lang.Object should not be allowed.");
        } catch (InvalidSubscriberException e) {
            // expected
        }
    }

    @Test
    public void testSubOnObjectAllowedForDynamicSub() throws Exception {

        EventBusImpl bus = eventBus;
        try {
            bus.registerSubscriber(new DynamicSubscriber() {

                @Subscribe
                @SuppressWarnings("unused")
                public void sub(Object king) {
                    //
                }

                @Override
                public Class<?> getEventType() {
                    return String.class;
                }
            });
        } catch (InvalidSubscriberException e) {
            throw new AssertionError("Subscriber on java.lang.Object should be allowed for dynamic subscribers.");
        }
    }

    @Test
    public void testNonObjectDynamicSub() throws Exception {

        EventBusImpl bus = eventBus;
        try {
            bus.registerSubscriber(new DynamicSubscriber() {

                @Subscribe
                @SuppressWarnings("unused")
                public void sub(Number king) {
                    //
                }

                @Override
                public Class<?> getEventType() {
                    return Double.class;
                }
            });
        } catch (InvalidSubscriberException e) {
            throw new AssertionError("Dynamic subscriber on incompatible event types allowed.");
        }
    }

    @Test
    public void testEventDispatchForDynamicSub() throws Exception {
        EventBusImpl bus = eventBus;
        final TestAwareQueue testAwareQueue = new TestAwareQueue();
        bus.setConsumerQueueSupplier(new EventBusImpl.ConsumerQueueSupplier() {
            @Override
            public ConsumerQueue get(Method subscriber, SubscriberConfigProvider.SubscriberConfig subscriberConfig, AtomicLong queueSizeCounter) {
                return testAwareQueue.setQueueSizeCounter(queueSizeCounter);
            }
        });
        Predicate<Object> filter = Predicates.or(
                new PathValueEventFilter("name", new StringValuePredicate("name")),
                new PathValueEventFilter("id", new NumericValuePredicate(1, "="))
        );

        DynamicHandler handler = new DynamicHandler(Event.class);
        bus.registerSubscriber(new InfixEventFilter(filter), handler);
        bus.publish(new Event("name", 1));

        checkIfEventOfferedAndDispatched(testAwareQueue, handler);
    }

    @Test
    public void testMismatchedEventNotDispatchForDynamicSub() throws Exception {
        EventBusImpl bus = eventBus;
        final TestAwareQueue testAwareQueue = new TestAwareQueue();
        bus.setConsumerQueueSupplier(new EventBusImpl.ConsumerQueueSupplier() {
            @Override
            public ConsumerQueue get(Method subscriber, SubscriberConfigProvider.SubscriberConfig subscriberConfig, AtomicLong queueSizeCounter) {
                return testAwareQueue.setQueueSizeCounter(queueSizeCounter);
            }
        });

        DynamicHandler handler = new DynamicHandler(Double.class);
        bus.registerSubscriber(handler);
        bus.publish(new Event("name", 1));

        assertEquals("Dynamic Handler received event it did not subscribe to!", 0, testAwareQueue.offeredCount.get());
    }

    @Test
    public void testSyncPublish() throws Exception {
        ((ConcurrentCompositeConfiguration)ConfigurationManager.getConfigInstance()).setOverrideProperty(
                SyncSubscribersGatekeeper.ALLOW_SYNC_SUBSCRIBERS, true);

        assertTrue("Allow sync subscriber property not set to true.",
                ConfigurationManager.getConfigInstance().getBoolean(SyncSubscribersGatekeeper.ALLOW_SYNC_SUBSCRIBERS, false));

        EventBusImpl bus = eventBus;
        final TestAwareQueue testAwareQueue = new TestAwareQueue();
        bus.setConsumerQueueSupplier(new EventBusImpl.ConsumerQueueSupplier() {
            @Override
            public ConsumerQueue get(Method subscriber, SubscriberConfigProvider.SubscriberConfig subscriberConfig, AtomicLong queueSizeCounter) {
                return testAwareQueue.setQueueSizeCounter(queueSizeCounter);
            }
        });

        SyncEventHandler handler = new SyncEventHandler();
        bus.registerSubscriber(handler);
        bus.publish(new Event("name", 1));

        assertEquals("Sync consumer offered asynchronously", 0, testAwareQueue.offeredCount.get());
        assertEquals("Sync consumer offered asynchronously", 1, handler.counterMockAnnotatable.get());
    }

    @Test
    public void testSyncPublishDynamic() throws Exception {
        ((ConcurrentCompositeConfiguration)ConfigurationManager.getConfigInstance()).setOverrideProperty(
                SyncSubscribersGatekeeper.ALLOW_SYNC_SUBSCRIBERS, true);

        assertTrue("Allow sync subscriber property not set to true.",
                ConfigurationManager.getConfigInstance().getBoolean(SyncSubscribersGatekeeper.ALLOW_SYNC_SUBSCRIBERS, false));

        EventBusImpl bus = eventBus;
        final TestAwareQueue testAwareQueue = new TestAwareQueue();
        bus.setConsumerQueueSupplier(new EventBusImpl.ConsumerQueueSupplier() {
            @Override
            public ConsumerQueue get(Method subscriber, SubscriberConfigProvider.SubscriberConfig subscriberConfig, AtomicLong queueSizeCounter) {
                return testAwareQueue.setQueueSizeCounter(queueSizeCounter);
            }
        });

        SyncEventHandler handler = new SyncEventHandler();
        bus.registerSubscriber(handler);


        bus.publish(new Event("name", 1));

        assertEquals("Sync consumer offered asynchronously", 0, testAwareQueue.offeredCount.get());
        assertEquals("Sync consumer offered asynchronously", 1, handler.counterMockAnnotatable.get());

        ((ConcurrentCompositeConfiguration)ConfigurationManager.getConfigInstance()).setOverrideProperty(
                SyncSubscribersGatekeeper.ALLOW_SYNC_SUBSCRIBERS, false);

        bus.publish(new Event("name", 1));

        assertEquals("Async override did not work.", 1, testAwareQueue.offeredCount.get());
    }


    public static void checkIfEventOfferedAndDispatched(TestAwareQueue testAwareQueue, EventHandler handler) {
        assertEquals("Event not offered.", 1, testAwareQueue.offeredCount.get());

        synchronized(handler.mockReceiveMonitor) {
            try {
                handler.mockReceiveMonitor.wait(1000);
            } catch (InterruptedException e) {
                // Do not bother, we just check for the condition
            }
        }
        assertEquals("Event not received by consumer in 1 sec after the offer.", 1, handler.counterMockAnnotatable.get());
    }

    public static void checkIfEventOfferedAndDispatched(TestAwareQueue testAwareQueue, DynamicHandler handler) {
        assertEquals("Event not offered.", 1, testAwareQueue.offeredCount.get());

        synchronized(handler.monitor) {
            try {
                handler.monitor.wait(1000);
            } catch (InterruptedException e) {
                // Do not bother, we just check for the condition
            }
        }
        assertEquals("Event not received by consumer in 1 sec after the offer.", 1, handler.receiveCounter.get());
    }

    public static void checkIfEventOfferedAndDispatched(TestAwareQueue testAwareQueue, Event2Handler handler) {
        assertEquals("", 1, testAwareQueue.offeredCount.get());

        synchronized(handler.event2ReceiveMonitor) {
            try {
                handler.event2ReceiveMonitor.wait(1000);
            } catch (InterruptedException e) {
                // Do not bother, we just check for the condition
            }
        }
        assertEquals("Event not received by consumer in 1 sec after the offer.", 1, handler.counterEvent2.get());
    }

	public static class EventHandler {

        final Object mockReceiveMonitor = new Object();
        protected final AtomicInteger counterMockAnnotatable = new AtomicInteger();

		@Subscribe
        @SuppressWarnings("unused")
		public void handleEvent(MockAnnotatable event){
			counterMockAnnotatable.incrementAndGet();
            synchronized (mockReceiveMonitor) {
                mockReceiveMonitor.notifyAll();
            }
        }
	}

	public static class StatefulEventHandler extends EventHandler {

        private String name;

        public StatefulEventHandler(String name) {
            this.name = name;
        }

        @Subscribe
        @SuppressWarnings("unused")
		public void handleEvent(MockAnnotatable event){
            super.handleEvent(event);
        }
	}

	public static class SyncEventHandler extends EventHandler {

        @Subscribe(syncIfAllowed = true)
        @SuppressWarnings("unused")
		public void handleEvent(MockAnnotatable event){
            super.handleEvent(event);
        }
	}

	public static class Event2Handler {

        final Object event2ReceiveMonitor = new Object();
        private final AtomicInteger counterEvent2 = new AtomicInteger();

		@Subscribe
        @SuppressWarnings("unused")
		public void handleEvent(Event2 event2){
			counterEvent2.incrementAndGet();
            synchronized (event2ReceiveMonitor) {
                event2ReceiveMonitor.notifyAll();
            }
        }
	}

	public static class DynamicHandler implements DynamicSubscriber {

        final Object monitor = new Object();
        private final AtomicInteger receiveCounter = new AtomicInteger();
        private Class<?> eventType;
        private Object lastEventReceived;

        public DynamicHandler(Class<?> eventType) {
            this.eventType = eventType;
        }

        @Subscribe
        @SuppressWarnings("unused")
		public void handleEvent(Object event){
			receiveCounter.incrementAndGet();
            synchronized (monitor) {
                lastEventReceived = event;
                monitor.notifyAll();
            }
        }

        @Override
        public Class<?> getEventType() {
            return eventType;
        }
    }

	// This has to be public in order for JXPath to find its instances' properties. The reason is
	// JXPath considers only public accessors. 
	public static class Event implements MockAnnotatable {
		private final String name;
		private final int id;
		
		public Event(String name, int id){
			this.name = name;
			this.id = id;
		}

        @SuppressWarnings("unused")
		public String getName() {
			return name;
		}
		
		public int getId() {
			return id;
		}
	}

	// This has to be public in order for JXPath to find its instances' properties. The reason is
	// JXPath considers only public accessors.
	public static class Event2{
		private final String name;
		private final int id;

		public Event2(String name, int id){
			this.name = name;
			this.id = id;
		}

        @SuppressWarnings("unused")
		public String getName() {
			return name;
		}

		public int getId() {
			return id;
		}
	}

    public static class TestAwareQueue implements EventBusImpl.ConsumerQueueSupplier.ConsumerQueue {

        AtomicInteger offeredCount = new AtomicInteger();
        LinkedBlockingQueue delegate = new LinkedBlockingQueue();
        AtomicLong queueSizeCounter;

        @Override
        @SuppressWarnings("unchecked")
        public boolean offer(Object o) {
            offeredCount.incrementAndGet();
            return delegate.offer(o);
        }

        @Override
        public Object nonBlockingTake() {
            return delegate.poll();
        }

        @Override
        public Object blockingTake() throws InterruptedException {
            return delegate.take();
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        public TestAwareQueue setQueueSizeCounter(AtomicLong queueSizeCounter) {
            this.queueSizeCounter = queueSizeCounter;
            return this;
        }
    }

    public static class TestAwareEventCreator implements EventCreator {

        AtomicInteger createEventCount = new AtomicInteger();
        Set<Class<?>> lastLiveEventTypes;
        private final List events;

        public TestAwareEventCreator(Object... eventsToReturn) {
            events = Arrays.asList(eventsToReturn);
        }

        @Override
        public List createEvent(Set<Class<?>> liveEventTypes) {
            createEventCount.incrementAndGet();
            lastLiveEventTypes = new HashSet<Class<?>>(liveEventTypes);
            return events;
        }
    }

    public static class TestAwareConsumerQueueSupplier implements EventBusImpl.ConsumerQueueSupplier {

        private TestAwareQueue testAwareQueue;

        @Override
        public ConsumerQueue get(Method subscriber, SubscriberConfigProvider.SubscriberConfig subscriberConfig, AtomicLong queueSizeCounter) {
            testAwareQueue = new TestAwareQueue();
            return testAwareQueue.setQueueSizeCounter(queueSizeCounter);
        }
    }
}
