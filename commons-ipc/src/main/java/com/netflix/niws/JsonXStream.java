/*
 * JsonXStream.java
 *  
 * $Header: $ 
 * $DateTime: $
 *
 * Copyright (c) 2009 Netflix, Inc.  All rights reserved.
 */
package com.netflix.niws;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * Per XStream FAQ - Once the XStream instance has been created and configured, it may be 
 * shared across multiple threads allowing objects to be serialized/deserialized 
 * concurrently. The creation and initialization of XStream is quite expensive, so this is
 * the singleton instance that we are reuse.
 * 
 * @author gkim
 */
public class JsonXStream extends AbstractXStream {
    
    private final static JsonXStream s_instance = new JsonXStream();    
    
    public JsonXStream() {
        super(new JettisonMappedXmlDriver());
        
        //registerConverter(new MetadataConverter());
        setMode(XStream.NO_REFERENCES);
        this.autodetectAnnotations(false);
        this.processAnnotations(IPayload.class);
    }
    
    public static JsonXStream getInstance() {
        return s_instance;
    }
    
}
