/*
*
* Copyright 2013 Netflix, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*/
package com.netflix.stats.distribution;

import java.util.concurrent.atomic.AtomicLong;


/**
 * This extends {@link Distribution Distribution} by tracking counts of
 * values per "bucket" and the ability to find the (approximate) median value.
 * <p>
 * Note that this implements {@link HistogramMBean} and so can be registered
 * as an MBean and accessed via JMX if desired.
 *
 * @author netflixoss $
 * @version $Revision: $
 */
public class Histogram extends Distribution implements HistogramMBean {

    private final double[] bucketLimits;
    private final AtomicLong[] bucketCounts;

    /*
     * Constructors
     */

    /**
     * Creates a new initially empty Histogram.
     *
     * @param bucketLimits an array of max values for each bucket;
     *    the values must be sorted in increasing order.
     *    The first bucket records values in the range
     *    <code>(-infinity .. bucketLimits[0])</code>,
     *    the last bucket records values in the range
     *    <code>(bucketLimits[bucketLimits.length-1] .. +infinity)</code>
     */
    public Histogram(double[] bucketLimits) {
        super();
        // Copy the supplied limits to ensure that they remain fixed
        this.bucketLimits = new double[bucketLimits.length];
        for (int i = 0; i < bucketLimits.length; i++) {
            this.bucketLimits[i] = bucketLimits[i];
        }
        // One extra bucket with implied max of +infinity
        bucketCounts = makeBuckets(bucketLimits.length + 1);
    }

    /**
     * Creates a new initially empty Histogram with
     * uniformally sized buckets.
     *
     * @param min the upper limit for the first bucket
     * @param max the upper limit for the final bucket
     *    (excluding the catch-all bucket)
     * @param step the size of the range of each bucket
     */
    public Histogram(double min, double max, double step) {
        super();
        bucketLimits = new double[1 + (int) Math.ceil((max - min) / step)];
        bucketLimits[0] = min;
        for (int i = 1; i < bucketLimits.length; i++) {
            bucketLimits[i] = bucketLimits[i - 1] + step;
        }
        // One extra bucket with implied max of +infinity
        bucketCounts = makeBuckets(bucketLimits.length + 1);
    }

    private AtomicLong[] makeBuckets(int cnt) {
        AtomicLong[] buckets = new AtomicLong[cnt];
        for (int i = 0; i < cnt; i++) {
            buckets[i] = new AtomicLong(0);
        }
        return buckets;
    }

    /*
     * Accumulating new values
     */

    /** {@inheritDoc} */
    @Override
    public void noteValue(double val) {
        super.noteValue(val);
        updateBucket(val, findBucket(val));
    }

    private int findBucket(double val) {
        // linear scan should be fast enough
        for (int i = 0; i < getNumBuckets(); i++) {
            if (val < getBucketMaximum(i)) {
                return i;
            }
        }
        // fall through if must use final catch-all bucket
        return getNumBuckets() - 1;
    }

    private void updateBucket(double val, int idx) {
        bucketCounts[idx].incrementAndGet();
    }

    private void clearBucket(int idx) {
        bucketCounts[idx].set(0);
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        super.clear();
        for (int idx = 0; idx < getNumBuckets(); idx++) {
            clearBucket(idx);
        }
    }

    /*
     * Accessors
     */

    /** {@inheritDoc} */
    public int getNumBuckets() {
        return bucketCounts.length;
    }


    /**
     * Gets the number of values recorded in a bucket.
     */
    public long getBucketCount(int i) {
 return bucketCounts[i].get();
    }

    /**
     * Gets the minimum for values recorded in a bucket.
     * This is an <em>inclusive</em> minimum; values equal to the
     * bucket limit are counted in this bucket.
     */
    public double getBucketMinimum(int i) {
        if (i > 0) {
            return bucketLimits[i - 1];
        } else if (getBucketCount(i) == 0) {
            // Edge case -- first bucket, but it is empty
            return Double.MIN_VALUE;
        } else {
            // First bucket is non-empty
            return getMinimum();
        }
    }

    /**
     * Gets the maximum for values recorded in a bucket.
     * This is an <em>exclusive</em> maximum; values equal to the
     * bucket limit are counted in the subsequent bucket.
     */
    public double getBucketMaximum(int i) {
        if (i < bucketLimits.length) {
            return bucketLimits[i];
        } else if (getBucketCount(i) == 0) {
            // last bucket, but empty
            return Double.MAX_VALUE;
        } else {
            return getMaximum();
        }
    }

    /** {@inheritDoc} */
    public long[] getBucketCounts() {
        long[] counts = new long[getNumBuckets()];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = getBucketCount(i);
        }
        return counts;
    }

    /** {@inheritDoc} */
    public double[] getBucketMinimums() {
        double[] mins = new double[getNumBuckets()];
        for (int i = 0; i < mins.length; i++) {
            mins[i] = getBucketMinimum(i);
        }
        return mins;
    }

    /** {@inheritDoc} */
    public double[] getBucketMaximums() {
        double[] maxs = new double[getNumBuckets()];
        for (int i = 0; i < maxs.length; i++) {
            maxs[i] = getBucketMaximum(i);
        }
        return maxs;
    }

    /** {@inheritDoc} */
    public double getMedian() {
        return getPercentile(50);             // SUPPRESS CHECKSTYLE MagicNumber
    }

    private double getNthValue(double n) {
        // A few simple cases to start off
        if (n <= 0) {
            return getMinimum();
        } else if (n >= (getNumValues() - 1)) {
            return getMaximum();
        }
        // Now on to the general case
        int bucket = 0;
        int lastBucket = getNumBuckets() - 1;
        double needed = n;
        // Skip to the bucket that contains the desired index
        while (needed > 0 && bucket < lastBucket && needed >= getBucketCount(bucket)) {
            needed -= getBucketCount(bucket);
            bucket++;
        }
        assert needed >= 0.0;
        /*
         * Find the desired value in the bucket.
         * Assume a smooth linear distribution of values in the bucket,
         * like this (for six values):
         *
         *     min                                       max
         *      x------x------x------x------x------x------|
         *      ^      ^      ^      ^      ^      ^
         *
         * Note that the values avoid the final endpoint.
         *
         * However, for the last bucket the true maximum is known
         * and so the distribution looks like:
         *
         *    min                                          max
         *     x--------x--------x--------x--------x--------x
         *     ^        ^        ^        ^        ^        ^
         */
        double min = getBucketMinimum(bucket);
        double max = getBucketMaximum(bucket);
        double tmp = getMinimum();
        if (min < tmp) {
            min = tmp;
        }
        tmp = getMaximum();
        if (max > tmp) {
            max = tmp;
        }
        assert min <= max;
        long count = getBucketCount(bucket);
        assert needed <= count;
        double value;
        if (bucket < lastBucket) {
            value = min + (max - min) * needed / count;
        } else if (count == 1) {
            // Final bucket, and only one entry (the max)
            value = max;
        } else {
            // Final bucket
            value = min + (max - min) * needed / (count - 1);
        }
        return value;
    }

    /** {@inheritDoc} */
    public double getPercentile(int percent) {
        if (getNumValues() == 0) {
            return getMinimum();
        } else {
            return getNthValue(getNumValues() * (percent / 100.0)); // SUPPRESS CHECKSTYLE MagicNumber
        }
    }

    private double getValueIndex(double value) {
        // A few simple cases to start off
        if (value <= getMinimum()) {
            return 0;
        } else if (value >= getMaximum()) {
            return getNumValues() - 1;
        }
        // Now on to the general case
        int bucket = 0;
        int lastBucket = getNumBuckets() - 1;
        double idx = 0.0;
        // Skip to the bucket that contains the desired value
        while (bucket < lastBucket && getBucketMaximum(bucket) <= value) {
            idx += getBucketCount(bucket);
            bucket++;
        }
        /*
         * Find the desired index in the bucket.
         * See comment in getNthValue() for details on the assumed
         * distribution of values.
         */
        double min = getBucketMinimum(bucket);
        double max = getBucketMaximum(bucket);
        long count = getBucketCount(bucket);
        assert min <= value;
        assert value < max;
        if (count == 0) {
            count = 0; // bad edge case -- just leave index alone (code in plae to suppress findbugs)
        } else if (bucket < lastBucket) {
            idx += count * (value - min) / (max - min);
        } else {
            idx += (count - 1) * (value - min) / (max - min);
        }
        assert idx >= 0;
        assert idx <= getNumValues() - 1;
        return idx;
    }

    /** {@inheritDoc} */
    public long getPercentileRank(double value) {
        // CHECKSTYLE IGNORE MagicNumber
        if (getNumValues() <= 1) {
            return 50;
        } else {
            return Math.round(getValueIndex(value) * 100.0 / (getNumValues() - 1));
        }
        // CHECKSTYLE END IGNORE MagicNumber
    }

} // Histogram
