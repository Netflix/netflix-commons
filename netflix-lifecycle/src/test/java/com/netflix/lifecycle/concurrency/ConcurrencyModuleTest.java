package com.netflix.lifecycle.concurrency;

import java.util.concurrent.ScheduledExecutorService;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.netflix.governator.annotations.binding.Background;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;

public class ConcurrencyModuleTest {
    @Test
    public void backgroundShouldBeInjectable() {
        Injector injector = LifecycleInjector.builder()
            .withRootModule(ConcurrencyModule.class)
            .build()
            .createInjector();

        ScheduledExecutorService service = injector.getInstance(Key.get(ScheduledExecutorService.class, Background.class));
    }
    
    @Test
    public void shouldUseOverrideModule() {
        Injector injector = LifecycleInjector.builder()
                .withRootModule(ConcurrencyModule.class)
                .withBootstrapModule(new BootstrapModule() {
                    @Override
                    public void configure(BootstrapBinder binder) {
                        binder.bind(ConcurrencyModule.class).toInstance(new ConcurrencyModule() {
                            @Override
                            protected void configure() {
                            }
                        });
                    }
                })
                .build()
                .createInjector();

        try {
            ScheduledExecutorService service = injector.getInstance(Key.get(ScheduledExecutorService.class, Background.class));
            Assert.fail("Binding shouldn't exist");
        }
        catch (ConfigurationException e) {
            
        }
    }
}
