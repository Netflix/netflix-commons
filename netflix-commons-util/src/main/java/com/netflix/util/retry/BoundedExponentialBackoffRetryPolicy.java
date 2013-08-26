/*******************************************************************************
 * Copyright 2011 Netflix
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.netflix.util.retry;

/**
 * Bounded exponential backoff that will wait for no more than a max amount of time.
 * 
 * @author elandau
 *
 */
public class BoundedExponentialBackoffRetryPolicy extends ExponentialBackoffRetryPolicy {
    private final long maxDelay;

    public BoundedExponentialBackoffRetryPolicy(long interval, long maxDelay, int maxAttempts) {
        super(interval, maxAttempts);
        this.maxDelay = maxDelay;
    }

    @Override
    protected long getBackoffDelay(Context context) {
        long delay = super.getBackoffDelay(context);
        if (delay == -1)
            return -1;
        
        return Math.min(delay, maxDelay);
    }
}
