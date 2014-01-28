package com.netflix.eventbus.impl;

import com.netflix.servo.monitor.Monitors;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math.stat.descriptive.rank.Percentile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Nitesh Kant (nkant@netflix.com)
 */
public abstract class AbstractEventBusStats {

    protected static final Logger LOGGER = LoggerFactory.getLogger(EventBusStats.class);

    protected long collectionDurationInMillis;

    // Static as the compute is CPU only task, so we can share these timers across consumers.
    protected static Timer statsSweeper = new Timer(true);

    public AbstractEventBusStats(long collectionDurationInMillis) {
        this.collectionDurationInMillis = collectionDurationInMillis;
        statsSweeper.schedule(new TimerTask() {
            @Override
            public void run() {
                computeTimeIntervalStats();
            }
        }, collectionDurationInMillis, collectionDurationInMillis);
    }

    protected void registerMonitors() {
        try {
            Monitors.registerObject(this);
        } catch (Throwable th) {
            LOGGER.error("Unable to register to event bus stats to servo.", th);
        }
    }

    protected abstract void computeTimeIntervalStats();

    protected class LatencyStats {

        // No reads happen on the below fields, it always happens via the computedData ref which is never written.
        private int sampleSize;
        private double mean;
        private double median;
        private double percentile_99_5;
        private double percentile_99;
        private double percentile_90;
        private double stddev;
        private double max;

        private ConcurrentLinkedQueue<Double> rawData;
        private AtomicReference<LatencyStats> computedData; // This is to avoid all members to be AtomicDoubles.

        protected LatencyStats() {
            rawData = new ConcurrentLinkedQueue<Double>();
            computedData = new AtomicReference<LatencyStats>(new LatencyStats(this));
        }

        private LatencyStats(LatencyStats copyFrom) {
            sampleSize = copyFrom.sampleSize;
            mean = copyFrom.mean;
            median = copyFrom.median;
            percentile_90 = copyFrom.percentile_90;
            percentile_99 = copyFrom.percentile_99;
            percentile_99_5 = copyFrom.percentile_99_5;
            stddev = copyFrom.stddev;
            max = copyFrom.max;
        }

        protected void addLatency(double latency) {
            rawData.add(latency);
        }

        protected void compute() {
            Percentile percentile = new Percentile();
            double[] rawDataAsArray = clearRawDataAndGetAsArray();
            if (null != rawDataAsArray && rawDataAsArray.length != 0) {
                sampleSize = rawDataAsArray.length;
                percentile.setData(rawDataAsArray);
                percentile_99_5 = percentile.evaluate(99.5);
                percentile_99 = percentile.evaluate(99);
                percentile_90 = percentile.evaluate(90);
                median = Math.max(1d, percentile.evaluate(50));
                max = StatUtils.max(rawDataAsArray);
                mean = new Mean().evaluate(rawDataAsArray);
                stddev = new StandardDeviation().evaluate(rawDataAsArray);
            }
            computedData.set(getCopyOfComputedData());
        }

        protected LatencyStats getComputedStats() {
            return computedData.get();
        }

        public int getSampleSize() {
            return getComputedStats().sampleSize;
        }

        public double getMean() {
            return getComputedStats().mean;
        }

        public double getMedian() {
            return getComputedStats().median;
        }

        public double getPercentile_99_5() {
            return getComputedStats().percentile_99_5;
        }

        public double getPercentile_99() {
            return getComputedStats().percentile_99;
        }

        public double getPercentile_90() {
            return getComputedStats().percentile_90;
        }

        public double getStddev() {
            return getComputedStats().stddev;
        }

        public double getMax() {
            return getComputedStats().max;
        }

        private LatencyStats getCopyOfComputedData() {
            return new LatencyStats(this);
        }

        private double[] clearRawDataAndGetAsArray() {
            double[] toReturn = new double[rawData.size()];
            int index = 0;
            for (Iterator<Double> iterator = rawData.iterator(); iterator.hasNext(); ) {
                Double aDataPoint = iterator.next();
                iterator.remove(); // Do not double count in the next cycle.
                toReturn[index++] = aDataPoint;
            }
            return toReturn;
        }
    }
}
