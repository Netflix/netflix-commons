/*
 *
 * Copyright 2015 Netflix, Inc.
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

package com.netflix.util.concurrent;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Unit tests for the ConcurrentUuidFactory class.
 */
public class ConcurrentUuidFactoryTest {
    @Test
    public void testGenerateRandomUuidVersionAndVariant() throws Exception {
        final UUID uuid = new ConcurrentUuidFactory().generateRandomUuid();

        Assert.assertEquals(4, uuid.version());
        Assert.assertEquals(2, uuid.variant());
    }

    @Test
    public void testGenerateRandomUuidNonConstant() throws Exception {
        final int numValues = 1000;
        final Set<UUID> values = new HashSet<>();
        final ConcurrentUuidFactory factory = new ConcurrentUuidFactory();

        for (int i = 0; i < numValues; ++i) {
            final UUID newUuid = factory.generateRandomUuid();
            Assert.assertTrue("Already generated UUID with value: " + newUuid, values.add(newUuid));
        }
        Assert.assertEquals(numValues, values.size());
    }
}
