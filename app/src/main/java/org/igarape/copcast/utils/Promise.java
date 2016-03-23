package org.igarape.copcast.utils;

/**
 * Created by martelli on 1/28/16.
 */
public abstract class Promise {

    public void success(Object payload) {};
    public void failure(Exception error) {};
    public void failure(String error_msg) {};
}
