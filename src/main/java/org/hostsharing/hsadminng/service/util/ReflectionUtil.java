package org.hostsharing.hsadminng.service.util;

import java.lang.reflect.Field;

public class ReflectionUtil {

    public static <T> void setValue(final T dto, final String fieldName, final Object value) {
        try {
            final Field field = dto.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(dto, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> void setValue(final T dto, final Field field, final Object value) {
        try {
            field.setAccessible(true);
            field.set(dto, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getValue(final T dto, final Field field) {
        try {
            field.setAccessible(true);
            return (T) field.get(dto);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    public static <T> T unchecked(final ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
