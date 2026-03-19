package net.hostsharing.hsadminng.mapper;

import jakarta.validation.ValidationException;
import java.util.Map;

public class KeyValueMap {

    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> from(final String key, final Object obj) {
        if (obj == null || obj instanceof Map<?, ?>) {
            return (Map<String, T>) obj;
        }
        throw new ValidationException(key + ": Map expected, but got: " + obj);
    }
}
