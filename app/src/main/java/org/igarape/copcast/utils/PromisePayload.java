package org.igarape.copcast.utils;

import java.util.HashMap;

/**
 * Created by martelli on 3/23/16.
 */
public class PromisePayload extends HashMap<String, Object> {

    public PromisePayload() {}

    public PromisePayload(String key, Object val) {
        super.put(key, val);
    }

}
