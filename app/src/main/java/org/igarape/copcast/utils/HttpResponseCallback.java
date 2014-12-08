package org.igarape.copcast.utils;

import org.json.JSONObject;

/**
 * Created by bruno on 11/5/14.
 */
public abstract class HttpResponseCallback {

    public abstract void unauthorized();

    public abstract void failure(int statusCode);

    public void success(JSONObject response) {
    }

    ;

    public abstract void noConnection();

    public abstract void badConnection();

    public abstract void badRequest();

    public abstract void badResponse();

    public void success(byte[] bufferedInputStream) {
    }
}
