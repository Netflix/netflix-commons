package com.netflix.lifecycle.concurrency;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PreDestroy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Provider;
import com.netflix.governator.guice.lazy.LazySingleton;

@LazySingleton
public class CoreCountBasedScheduledExecutorServiceProvider implements Provider<ScheduledExecutorService> {
    private ScheduledExecutorService service;
    
    @Override
    public ScheduledExecutorService get() {
        service = Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors(), 
                new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("Background-%d")
                    .build());
        return service;
    }

    @PreDestroy
    protected void shutdown() {
        if (service != null)
            service.shutdown();
    }
}
