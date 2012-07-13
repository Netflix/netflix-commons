package com.netflix.niws;

import com.netflix.util.Pair;

import static org.junit.Assert.*;

import org.junit.Test;

public class JsonXStreamTest {
    
    @Test
    public void testJsonXStream() {
        Pair<String, String> pair = new Pair<String, String>("a", "b");
        assertNotNull(pair);
        JsonXStream obj = new JsonXStream();
        assertNotNull(obj);
    }
}
