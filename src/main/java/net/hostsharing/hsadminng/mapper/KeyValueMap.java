package net.hostsharing.hsadminng.mapper;

import java.util.Map;

public class KeyValueMap {

    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> from(final Object obj) {
        if (obj == null || obj instanceof Map<?, ?>) {
            return (Map<String, T>) obj;
        }
        throw new ClassCastException("Map expected, but got: " + obj);
    }
}
