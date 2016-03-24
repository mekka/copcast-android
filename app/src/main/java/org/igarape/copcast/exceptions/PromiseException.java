package org.igarape.copcast.exceptions;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by martelli on 3/23/16.
 */
public class PromiseException<T> {
    private T failure;
    private HashMap<String, Object> paramBag = new HashMap<>();

    public PromiseException(T failure) {
        this.failure = failure;
    }

    public PromiseException(T failure, String key, Object val) {
        this.failure = failure;
        this.paramBag.put(key, val);
    }

    public T getFailure() {
        return failure;
    }

    public void put(String key, Object value) {
        paramBag.put(key, value);
    }

    public Object get(String key) {
        return paramBag.get(key);
    }

    public Set<String> keySet() {
        return paramBag.keySet();
    }

}
