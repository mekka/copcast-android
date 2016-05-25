package org.igarape.copcast.promises;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by martelli on 3/24/16.
 */
public enum HttpPromiseError implements PromiseError {

    NO_CONNECTION,
    FORBIDDEN,
    NOT_AUTHORIZED,
    BAD_REQUEST,
    BAD_CONNECTION,
    BAD_RESPONSE,
    JSON_ERROR,
    SIGNING_ERROR,
    FAILURE;

    private HashMap<String, Object> bag = new HashMap<>();

    public HttpPromiseError put(String key, Object value) {
        bag.put(key, value);
        return this;
    }

    public Object get(String key) {
        return bag.get(key);
    }

    public Set<String> keySet() {
        return bag.keySet();
    }
}
