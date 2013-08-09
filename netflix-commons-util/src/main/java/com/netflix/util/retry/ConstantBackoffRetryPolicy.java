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

import java.util.concurrent.TimeUnit;

/**
 * Retry policy with constant wait time between attempts.
 * 
 * @author elandau
 *
 */
public class ConstantBackoffRetryPolicy extends CountingRetryPolicy {

    private final long delay;

    public ConstantBackoffRetryPolicy(int maxAttemptCount, long delay) {
        super(maxAttemptCount);
        this.delay = delay;
    }

    public ConstantBackoffRetryPolicy(int maxAttemptCount, long delay, TimeUnit units) {
        this(maxAttemptCount, units.toMillis(delay));
    }

    @Override
    public long nextBackoffDelay(int attempt, long elapsedMillis) {
        if (super.nextBackoffDelay(attempt, elapsedMillis) == -1)
            return -1;
        
        System.out.println("Attempt " + attempt);
        return delay;
    }
}
