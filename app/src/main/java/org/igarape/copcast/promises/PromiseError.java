package org.igarape.copcast.promises;

import java.util.Set;

/**
 * Created by martelli on 3/24/16.
 */
public interface PromiseError {

    PromiseError put(String key, Object value);
    Object get(String key);
    Set<String> keySet();

}
