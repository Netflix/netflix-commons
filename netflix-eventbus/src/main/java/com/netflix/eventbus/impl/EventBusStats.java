package com.netflix.eventbus.impl;

import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.monitor.StatsTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.netflix.eventbus.utils.EventBusUtils.newStatsTimer;

/**
 * Runtime statistics for event bus, this relates to publish data and is always captured for the time period as returned
 * by {@link EventBusStats#}.
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
class EventBusStats {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBusStats.class);

    final StatsTimer publishStats;
    final StatsTimer conditionalPublishStats;
    final StatsTimer filterStats;
    final Counter publishErrors;
    final Counter conditionalPublishErrors;

    public EventBusStats(long collectionDurationInMillis) {
        publishStats = newStatsTimer("eventbus_publish", collectionDurationInMillis);
        conditionalPublishStats = newStatsTimer("eventbus_conditional_publish", collectionDurationInMillis);
        filterStats = newStatsTimer("eventbus_publish_filter_stats", collectionDurationInMillis);
        publishErrors = new BasicCounter(MonitorConfig.builder("eventbus_publish_errors").build());
        conditionalPublishErrors = new BasicCounter(MonitorConfig.builder("eventbus_conditional_publish_errors").build());
        try {
            Monitors.registerObject(this);
        } catch (Throwable th) {
            LOGGER.error("Unable to register to event bus stats to servo.", th);
        }
    }
}
