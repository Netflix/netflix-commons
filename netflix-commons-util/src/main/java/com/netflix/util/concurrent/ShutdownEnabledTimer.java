package com.netflix.util.concurrent;

import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * 
 * ShutdownEnabledTimer class handles runtime shutdown issues.
 * 
 * Apparently, adding a runtime shutdown hook will create a global reference
 * which can cause memory leaks if not cleaned up.
 * 
 * This abstraction provides a wrapped mechanism to manage those runtime shutdown hooks.
 * 
 * @author jzarfoss
 *
 */
public class ShutdownEnabledTimer extends Timer{

	
	private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownEnabledTimer.class);
	
	private Thread cancelThread;
	private String name;
	
	public ShutdownEnabledTimer(String name, boolean daemon){
		super(name, daemon);
		
		this.name = name;
		
		this.cancelThread = new Thread(new Runnable() {
	           public void run()
	           {
	        	   ShutdownEnabledTimer.super.cancel();
	           }
	       });
		
		LOGGER.info("Shutdown hook installed for: {}", this.name);
		
		Runtime.getRuntime().addShutdownHook(this.cancelThread);
	}
	
	@Override
	public void cancel(){
		super.cancel();
		
		LOGGER.info("Shutdown hook removed for: {}", this.name);
		
		try{
			Runtime.getRuntime().removeShutdownHook(this.cancelThread);
		}catch(IllegalStateException ise){
			LOGGER.info("Exception caught (might be ok if at shutdown)", ise);
		}
	
	}
	
}
