package com.netflix.eventbus.persistence;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * A utility class to serialize persistence data.
 *
 * @author Nitesh Kant
 */
public class Serializer {

    private static final Gson gson = new Gson();

    public static String serializeAsJson(PersistedData data) throws UnsupportedEncodingException {
        String json = gson.toJson(data);
        return URLEncoder.encode(json, "UTF-8");
    }

    public static PersistedData deserializeDataFromJson(String json) throws UnsupportedEncodingException {
        Preconditions.checkNotNull(json);
        String decoded = URLDecoder.decode(json, "UTF-8");
        return gson.fromJson(decoded, PersistedData.class);
    }
}
