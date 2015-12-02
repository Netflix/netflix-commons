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

package com.netflix.util;

import org.junit.Test;

import java.util.HashSet;
import java.util.UUID;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;

/**
 * @author Mike Smith
 */
public class ThreadLocalUUIDTest
{
    @Test
    public void testGenerateUUID() throws Exception {

        // Validate Java can parse this UUID and it's version 4 and variant 2.
        String uuid = ThreadLocalUUID.randomUUID().toString();
        try {
            UUID guuid = UUID.fromString(uuid);
            assertEquals(2, guuid.variant());
            assertEquals(4, guuid.version());
        } catch(IllegalArgumentException iae) {
            fail(iae.toString());
        }

        // Quick check that they're unique.
        HashSet<String> uuids = new HashSet<>();
        for (int i=0; i<1000; i++) {
            uuids.add(ThreadLocalUUID.randomUUID().toString());
        }
        assertEquals(1000, uuids.size());
    }
}
