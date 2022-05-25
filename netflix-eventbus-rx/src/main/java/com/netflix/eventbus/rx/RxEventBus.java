package com.netflix.eventbus.rx;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.netflix.eventbus.spi.DynamicSubscriber;
import com.netflix.eventbus.spi.EventBus;
import com.netflix.eventbus.spi.EventCreator;
import com.netflix.eventbus.spi.EventFilter;
import com.netflix.eventbus.spi.InvalidSubscriberException;
import com.netflix.eventbus.spi.Subscribe;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.BooleanSubscription;

/**
 * Small wrapper on top of the EventBus to allow consumption of events as
 * Rx streams.  
 * 
 * @author elandau
 *
 */
@Singleton
public class RxEventBus {
    private final EventBus eventBus;
    
    @Inject
    public RxEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    /**
     * {@link EventBus#publish(Object)}
     */
    public void publish(Object event) {
        eventBus.publish(event);
    }
    
    /**
     * {@link EventBus#publishIffNotDead(EventCreator, Class[])}
     */
    public void publishIfNotDead(EventCreator creator, Class<?>... eventTypes) {
        eventBus.publishIffNotDead(creator, eventTypes);
    }
    
    /**
     * Create an observable for this eventType.  A new event bus subscription
     * is made for each call to the Observables.subscribe().  The subscription
     * is removed from the underlying EventBus when unsubscribe is called.
     * 
     * @param eventType
     * @return
     */
    public <T> Observable<T> asObservable(final Class<T> eventType) {
        return Observable.create(new OnSubscribe<T>() {
            @Override
            public void call(final Subscriber<? super T> observer) {
                final DynamicSubscriber consumer = new DynamicSubscriber() {
                    @Override
                    public Class<?> getEventType() {
                        return eventType;
                    }
                    
                    @Subscribe
                    public void consume(Object obj) {
                        observer.onNext((T) obj);
                    }
                };
                
                observer.add(BooleanSubscription.create(new Action0() {
                    @Override
                    public void call() {
                        eventBus.unregisterSubscriber(consumer);
                        observer.onCompleted();
                    }
                }));
                
                try {
                    eventBus.registerSubscriber(consumer);
                } catch (InvalidSubscriberException e) {
                    observer.onError(e);
                }
            }
        });
    }
    
    /**
     * Create an observable for this eventType using a filter.  A new event bus 
     * subscription is made for each call to the Observables.subscribe().  The 
     * subscription is removed from the underlying EventBus when unsubscribe is 
     * called.
     * 
     * @param eventType
     * @return
     */
    public <T> Observable<T> asObservable(final Class<T> eventType, final EventFilter filter) {
        return Observable.create(new OnSubscribe<T>() {
            @Override
            public void call(final Subscriber<? super T> observer) {
                final DynamicSubscriber consumer = new DynamicSubscriber() {
                    @Override
                    public Class<?> getEventType() {
                        return eventType;
                    }
                    
                    @Subscribe
                    public void consume(Object obj) {
                        observer.onNext((T) obj);
                    }
                };
                
                observer.add(BooleanSubscription.create(new Action0() {
                    @Override
                    public void call() {
                        eventBus.unregisterSubscriber(consumer);
                        observer.onCompleted();
                    }
                }));
                
                try {
                    eventBus.registerSubscriber(filter, consumer);
                } catch (InvalidSubscriberException e) {
                    observer.onError(e);
                }
            }
        });
    }

}
