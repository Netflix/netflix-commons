package com.netflix.infix.lang.infix.antlr;

public class EventFilterParsingException extends RuntimeException {
	public EventFilterParsingException(String msg, Throwable cause){
		super(msg, cause);
	}
}
