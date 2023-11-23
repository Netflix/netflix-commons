package com.netflix.jersey.guice.providers.exception;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Jersey does not provide a mechanism for multiple exception mappers to exist for the same 
 * Exception type.  Consequently  it is not possible to implement context specific mapping 
 * customizations. This is needed for multi-tenant http servers with a single server context 
 * where each resource may wish to implement a different exception format.  
 * 
 * ThrowableExceptionMapper solves this by exposing a Guice multi-binding based 'plugin' 
 * architecture where a custom mapper can be added via simple Guice binding.  When an 
 * exception occurs each CustomThrowableExceptionMapper is asked to determine if it 
 * can/should map the exception.  A CustomThrowableExceptionMapper may inspect the request 
 * context (url, content type, etc...) as well as the actual exception and even stack trace 
 * to determine if it should map the exception and format the response.  A 
 * DefaultThrowableExceptionMapper is called if no custom mapper is matched.  
 * 
 * Note that this should ONLY be used for the catch all exception mapping.  Specific exceptions
 * should be mapped by creating a specific {@code ExceptionMapper<MyException>. }
 * 
 * To enable a custom mapper just add a binding when bootstrapping guice.
 * <pre>
 * {@code
 *  new AbstractModule() {
 *      void configuration() {
 *          Multibinder<CustomThrowableExceptionMapper> mappers = Multibinder.newSetBinder(binder(), CustomThrowableExceptionMapper.class);
 *          mappers.addBinding().to(MyCustomExceptionMapper.class);
 *      }
 *  }
 * }
 * </pre>
 * 
 * The default mapper may also be replaced via the following Guice binding.  
 * 
 * <pre>
 * {@code
 *  new AbstractModule() {
 *      void configuration() {
 *          bind(DefaultThrowableExceptionMapper.class).to(MyDefaultThrowableExceptionMapper.class);
 *      }
 *  }
 * }
 * </pre>
 * 
 * @see GsonDefaultExceptionMapper
 * @author elandau
 *
 */
@Provider
@Singleton
public class ThrowableExceptionMapper implements ExceptionMapper<Throwable> {
    private final DefaultThrowableExceptionMapper defaultMapper;
    private final Set<CustomThrowableExceptionMapper> mappers;
    
    @Context 
    ThreadLocal<HttpServletRequest> request = new ThreadLocal<HttpServletRequest>() ;
    
    @Inject
    public ThrowableExceptionMapper(Set<CustomThrowableExceptionMapper> mappers, DefaultThrowableExceptionMapper defaultMapper) {
        this.mappers = mappers;
        this.defaultMapper = defaultMapper;
    }
    
    @Override
    public Response toResponse(Throwable exception) {
        HttpServletRequest request = this.request.get(); 
        for (CustomThrowableExceptionMapper mapper : mappers) {
            try {
                if (mapper.canMap(exception, request)) {
                    return mapper.toResponse(exception);
                }
            }
            catch (Throwable t) {
                // OK to ignore
            }
        }
        return defaultMapper.toResponse(exception);
    }

}
