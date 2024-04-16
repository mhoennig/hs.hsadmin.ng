package net.hostsharing.hsadminng.mapper;

import java.util.Map;

public class KeyValueMap {

    @SuppressWarnings("unchecked")
    public static Map<String, Object> from(final Object obj) {
        if (obj instanceof Map<?, ?>) {
            return (Map<String, Object>) obj;
        }
        throw new ClassCastException("Map expected, but got: " + obj);
    }
}
