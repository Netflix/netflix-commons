package com.netflix.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class PairTest {

    @Test
    public void testPair() {
        Pair<String, String> pair = new Pair<String, String>("a", "b");
        assertNotNull(pair);
    }
}
