package com.netflix.niws;

import java.util.concurrent.ConcurrentHashMap;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;

public class AbstractXStream extends XStream {
    private static final Object VALUE = new Object();
    private ConcurrentHashMap<String, Object> processedAnnotations = null;
    
    public AbstractXStream(HierarchicalStreamDriver hierarchicalStreamDriver) {
        super(hierarchicalStreamDriver);
        processedAnnotations = new ConcurrentHashMap<String, Object>();
        this.processAnnotations(IPayload.class);
    }
        
    @Override
    public void processAnnotations(Class type) {
        String name = type.getName();
        if (!processedAnnotations.containsKey(name)) {
            super.processAnnotations(type);
            processedAnnotations.putIfAbsent(name, VALUE);
        } 
    }

}
