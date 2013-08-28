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

import java.util.Random;

/**
 * Unbounded exponential backoff will sleep a random number of intervals within an
 * exponentially increasing number of intervals.  
 * 
 * @author elandau
 *
 */
public class ExponentialBackoffRetryPolicy extends CountingRetryPolicy {
    private final int MAX_SHIFT = 30;
    
    private final static Random random = new Random();
    private final long interval;

    public ExponentialBackoffRetryPolicy(long interval, int maxAttempts) {
        super(maxAttempts);
        this.interval = interval;
    }

    @Override
    protected long getBackoffDelay(Context context) {
        if (super.getBackoffDelay(context) == -1)
            return -1;
        
        // Avoid int overflow.  
        int attempt = (int) Math.min(MAX_SHIFT, context.getAttemptCount());
        
        // Determine a random number of internals up to 2^attempt
        int exp = random.nextInt(1 << attempt);
        
        // Return actual time based on interval
        return interval * exp;
    }
}
