package com.netflix.eventbus.impl;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.BasicGauge;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.StatsTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import static com.netflix.eventbus.utils.EventBusUtils.newStatsTimer;

/**
 * @author Nitesh Kant (nkant@netflix.com)
 */
public class EventConsumerStats {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventConsumerStats.class);

    final StatsTimer enqueueStats;
    final StatsTimer consumptionStats;
    final StatsTimer filterStats;
    final AtomicLong QUEUE_SIZE_COUNTER;
    final BasicGauge<Long> QUEUE_SIZE_GAUGE;
    final Counter QUEUE_OFFER_RETRY_COUNTER;
    final Counter EVENT_ENQUEUE_REJECTED_COUNTER;

    public EventConsumerStats(String consumerName, long collectionDurationInMillis) {
        String statsPrefix = "eventbus_consumer_" + consumerName;
        QUEUE_SIZE_COUNTER = new AtomicLong();
        QUEUE_SIZE_GAUGE = new BasicGauge<Long>(MonitorConfig.builder(statsPrefix + "_queue_size").build(), new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return QUEUE_SIZE_COUNTER.get();
            }
        });
        QUEUE_OFFER_RETRY_COUNTER = new BasicCounter(MonitorConfig.builder(statsPrefix + "_queue_retry").build());
        EVENT_ENQUEUE_REJECTED_COUNTER = new BasicCounter(MonitorConfig.builder(statsPrefix + "_enqueue_reject").build());

        enqueueStats = newStatsTimer(statsPrefix + "_enqueue", collectionDurationInMillis);
        consumptionStats = newStatsTimer(statsPrefix + "_consumption", collectionDurationInMillis);
        filterStats = newStatsTimer(statsPrefix + "_filter", collectionDurationInMillis);
        try {
            DefaultMonitorRegistry.getInstance().register(QUEUE_SIZE_GAUGE);
            DefaultMonitorRegistry.getInstance().register(QUEUE_OFFER_RETRY_COUNTER);
            DefaultMonitorRegistry.getInstance().register(EVENT_ENQUEUE_REJECTED_COUNTER);
            DefaultMonitorRegistry.getInstance().register(enqueueStats);
            DefaultMonitorRegistry.getInstance().register(consumptionStats);
            DefaultMonitorRegistry.getInstance().register(filterStats);
        } catch (Throwable th) {
            LOGGER.error("Unable to register to event bus consumer stats to servo.", th);
        }
    }
}
