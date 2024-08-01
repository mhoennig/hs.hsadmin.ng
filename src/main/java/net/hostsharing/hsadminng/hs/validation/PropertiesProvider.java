package net.hostsharing.hsadminng.hs.validation;

import java.util.Map;

public interface PropertiesProvider {

    boolean isLoaded();
    Map<String, Object> directProps();
    Object getContextValue(final String propName);

    default <T> T getDirectValue(final String propName, final Class<T> clazz) {
        return cast(propName, directProps().get(propName), clazz, null);
    }

    default <T> T getDirectValue(final String propName, final Class<T> clazz, final T defaultValue) {
        return cast(propName, directProps().get(propName), clazz, defaultValue);
    }

    default <T>  T getContextValue(final String propName, final Class<T> clazz) {
        return cast(propName, getContextValue(propName), clazz, null);
    }

    default <T>  T getContextValue(final String propName, final Class<T> clazz, final T defaultValue) {
        return cast(propName, getContextValue(propName), clazz, defaultValue);
    }

    private static <T> T cast( final String propName, final Object value, final Class<T> clazz, final T defaultValue) {
        if (value == null && defaultValue != null) {
            return defaultValue;
        }
        if (value == null || clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        throw new IllegalStateException(propName + " expected to be an "+clazz.getSimpleName()+", but got '" + value + "'");
    }
}
