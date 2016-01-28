package org.igarape.copcast.utils;

/**
 * Created by martelli on 1/28/16.
 */
public abstract class Promise {

    public abstract void success(Object payload);
    public abstract void failure(Object error);
}
