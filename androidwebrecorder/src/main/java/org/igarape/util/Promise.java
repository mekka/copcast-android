package org.igarape.util;

/**
 * Created by martelli on 2/15/16.
 */
public abstract class Promise {

    public abstract void success(Object payload);
    public abstract void failure(Exception exception);

    public void success() {
        success(null);
    }
}