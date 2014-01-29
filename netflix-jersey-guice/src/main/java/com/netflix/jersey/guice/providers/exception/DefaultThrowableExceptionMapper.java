package com.netflix.jersey.guice.providers.exception;

import javax.ws.rs.ext.ExceptionMapper;

import com.google.inject.ImplementedBy;

@ImplementedBy(GsonDefaultExceptionMapper.class)
public interface DefaultThrowableExceptionMapper extends ExceptionMapper<Throwable> {

}
