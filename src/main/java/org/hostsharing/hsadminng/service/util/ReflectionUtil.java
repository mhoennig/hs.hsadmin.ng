package org.hostsharing.hsadminng.service.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ReflectionUtil {

    public static Field getField(final Class<?> aClass, final String fieldName) {
        try {
            return aClass.getDeclaredField(fieldName);
        } catch (final NoSuchFieldException e) {
            if (aClass.getSuperclass() != Object.class) {
                return getField(aClass.getSuperclass(), fieldName);
            }
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> void setValue(final T dto, final Field field, final Object value) {
        try {
            field.setAccessible(true);
            field.set(dto, value);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, R> R getValue(final T dto, final Field field) {
        try {
            field.setAccessible(true);
            return (R) field.get(dto);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Searches the annotations of 'clazz' for an implemented interface 'rawInterface' and returns the class of the actual generics parameter at the specified index.
     *
     * @param clazz        a class which implements the generic interface 'rawInterface'
     * @param rawInterface a generic interface
     * @param paramIndex   the index of the generics parameter within 'rawInterface'
     * @param <T>          the expected class of the generics parameter at position 'index' in 'rawInterface'
     * @return the actual generics parameter
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> determineGenericInterfaceParameter(final Class<?> clazz, final Class<?> rawInterface, final int paramIndex) {
        for (Type genericInterface : clazz.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType) {
                final ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                if (parameterizedType.getRawType() == rawInterface) {
                    return (Class<T>) parameterizedType.getActualTypeArguments()[paramIndex];
                }
            }
        }
        if (clazz.getSuperclass() != Object.class) {
            return determineGenericInterfaceParameter(clazz.getSuperclass(), rawInterface, paramIndex);
        }
        for (Class<?> implementedInterface : clazz.getInterfaces()) {
            final Class<T> found = determineGenericInterfaceParameter(implementedInterface, rawInterface, paramIndex);
            if (found != null) {
                return found;
            }
        }
        throw new AssertionError(clazz.getSimpleName() + " expected to implement " + rawInterface.getSimpleName() + "<...>");
    }

    /**
     * Searches the annotations of 'clazz' for an extended class 'rawClass' and returns the class of the actual generics parameter at the specified index.
     *
     * @param clazz      a class which implements the generic interface 'rawClass'
     * @param rawClass   a generic class
     * @param paramIndex the index of the generics parameter within 'rawClass'
     * @param <T>        the expected class of the generics parameter at position 'index' in 'rawClass'
     * @return the actual generics parameter
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> determineGenericClassParameter(final Class<?> clazz, final Class<?> rawClass, final int paramIndex) {
        final Type genericClass = clazz.getGenericSuperclass();
        if (genericClass instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) genericClass;
            if (parameterizedType.getRawType() == rawClass) {
                return (Class<T>) parameterizedType.getActualTypeArguments()[paramIndex];
            }
        }
        if (clazz.getSuperclass() != Object.class) {
            return determineGenericClassParameter(clazz.getSuperclass(), rawClass, paramIndex);
        }
        throw new AssertionError(clazz.getSimpleName() + " expected to extend " + rawClass.getSimpleName() + "<...>");
    }

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> Enum<E> asEnumValue(final Class<?> type, final Object value) {
        return Enum.valueOf((Class<E>) type, value.toString());
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    public static <T> T unchecked(final ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
