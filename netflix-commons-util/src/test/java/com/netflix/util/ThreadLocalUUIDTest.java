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
