package org.igarape.copcast.utils;

import org.igarape.copcast.exceptions.PromiseException;

/**
 * Created by martelli on 1/28/16.
 */
public abstract class Promise<T> {

    public void success() {
        success(null);
    }
    public void success(PromisePayload payload) {}
    public void error(PromiseException<T> error) {}
    public void error(String error_msg) {}
}
