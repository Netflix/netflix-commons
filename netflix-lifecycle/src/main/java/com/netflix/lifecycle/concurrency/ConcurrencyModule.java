package com.netflix.lifecycle.concurrency;

import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.netflix.governator.annotations.binding.Background;

/**
 * Provide bindings for concurrency related singletons such as background
 * executors.  All bindings should be LazySingletons so that they are only 
 * created when needed.
 * 
 * @author elandau
 *
 */
@Singleton
public class ConcurrencyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ScheduledExecutorService.class)
            .annotatedWith(Background.class)
            .toProvider(CoreCountBasedScheduledExecutorServiceProvider.class);
    }
}
