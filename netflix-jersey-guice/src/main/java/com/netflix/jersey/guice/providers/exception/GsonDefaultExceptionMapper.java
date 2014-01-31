package com.netflix.jersey.guice.providers.exception;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import com.google.gson.Gson;

/**
 * Default mapper returns error 500 and a JSON response containing,
 * {
 *  code : 500,
 *  class : ...
 *  stack : ...
 * }
 * 
 * An alternative default mapper can be used by binding the implementation
 * to DefaultThrowableExceptionMapper.class in a guice module.
 * 
 * @author elandau
 *
 */
public class GsonDefaultExceptionMapper implements DefaultThrowableExceptionMapper {

    @Override
    public Response toResponse(Throwable exception) {
        Map<String, Object> info = new HashMap<String, Object>();
        if (exception.getCause() != null)
            info.put("class", exception.getCause().getClass().getCanonicalName());
        else 
            info.put("class", exception.getClass().getCanonicalName());
        info.put("code",  500);
        info.put("stack", exception.getStackTrace());
        
        return Response.status(500).entity(new Gson().toJson(info)).build();
    }


}
