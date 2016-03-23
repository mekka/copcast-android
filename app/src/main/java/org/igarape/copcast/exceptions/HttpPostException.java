package org.igarape.copcast.exceptions;

/**
 * Created by martelli on 3/23/16.
 */
public enum HttpPostException {

    NO_CONNECTION,
    FORBIDDEN,
    UNAUTHORIZED,
    UNKNOWN;

    public Exception asException() {
        return new Exception(this.toString());
    }

    public HttpPostException fromException(Exception e) {
        return valueOf(e.getMessage());
    }
}
