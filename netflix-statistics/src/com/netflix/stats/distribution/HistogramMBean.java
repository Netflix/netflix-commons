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


/**
 * Abstract MBean interface for objects that describe the general
 * characteristics of a distribution of (double) values recorded
 * as a histogram.
 * This interface supports the standard MBean management interface,
 * so implementing classes will support JMX monitoring.
 *
 * @author $Author: netflixoss $
 * @version $Revision: $
 */
public interface HistogramMBean extends DistributionMBean {

    /**
     * Gets the total number of buckets.
     * Note that this includes any buckets added to handle out-of-range values.
     */
    int getNumBuckets();

    /**
     * Gets the number of values recorded in each bucket.
     */
    long[] getBucketCounts();

    /**
     * Gets the minimum bound for the histogram buckets.
     * This is an <em>inclusive</em> minimum; values equal to the
     * bucket limit are counted in this bucket.
     */
    double[] getBucketMinimums();

    /**
     * Gets the maximum bound for the histogram buckets.
     * This is an <em>exclusive</em> maximum; values equal to the
     * bucket limit are counted in the subsequent bucket.
     */
    double[] getBucketMaximums();

    /**
     * Gets the approximate median value, that is the value where half of the
     * observed values are less than the median and half are greater.
     */
    double getMedian();

    /**
     * Gets the (approximate) percentile value, that is the value where some
     * desired percent of the observed values are less than the percentile
     * value and the remainder are greater.
     */
    double getPercentile(int percent);

    /**
     * Gets the (approximate) percentage of observed values that are
     * less than a given value.
     */
    long getPercentileRank(double value);

} // HistogramMBean
