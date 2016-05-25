package org.igarape.copcast.promises;

import java.util.HashMap;
import java.util.Set;


public enum WebRecorderPromiseError implements PromiseError {

    OTHER;

    private HashMap<String, Object> bag = new HashMap<>();

    public WebRecorderPromiseError put(String key, Object value) {
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
