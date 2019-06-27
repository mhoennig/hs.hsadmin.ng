// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.util;

import org.hostsharing.hsadminng.service.accessfilter.Role;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Function;

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
     * Searches the annotations of 'clazz' for an implemented interface 'rawInterface' and returns the class of the actual
     * generics parameter at the specified index.
     *
     * @param clazz a class which implements the generic interface 'rawInterface'
     * @param rawInterface a generic interface
     * @param paramIndex the index of the generics parameter within 'rawInterface'
     * @param <T> the expected class of the generics parameter at position 'index' in 'rawInterface'
     * @return the actual generics parameter
     */
    public static <T> Class<T> determineGenericInterfaceParameter(
            final Class<?> clazz,
            final Class<?> rawInterface,
            final int paramIndex) {
        final Class<T> found = determineGenericInterfaceParameterImpl(clazz, rawInterface, paramIndex);
        if (found == null) {
            throw new AssertionError(
                    clazz.getSimpleName() + " expected to implement " + rawInterface.getSimpleName() + "<...>");
        }
        return found;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> determineGenericInterfaceParameterImpl(
            final Class<?> clazz,
            final Class<?> rawInterface,
            final int paramIndex) {
        for (Type genericInterface : clazz.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType) {
                final ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                if (parameterizedType.getRawType() == rawInterface) {
                    return (Class<T>) parameterizedType.getActualTypeArguments()[paramIndex];
                }
            }
        }
        if (clazz.getSuperclass() != null) {
            final Class<T> found = determineGenericInterfaceParameterImpl(clazz.getSuperclass(), rawInterface, paramIndex);
            if (found != null) {
                return found;
            }
        }
        for (Class<?> implementedInterface : clazz.getInterfaces()) {
            final Class<T> found = determineGenericInterfaceParameterImpl(implementedInterface, rawInterface, paramIndex);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Searches the annotations of 'clazz' for an extended class 'rawClass' and returns the class of the actual generics
     * parameter at the specified index.
     *
     * @param clazz a class which implements the generic interface 'rawClass'
     * @param rawClass a generic class
     * @param paramIndex the index of the generics parameter within 'rawClass'
     * @param <T> the expected class of the generics parameter at position 'index' in 'rawClass'
     * @return the actual generics parameter
     */
    public static <T> Class<T> determineGenericClassParameter(
            final Class<?> clazz,
            final Class<?> rawClass,
            final int paramIndex) {
        final Class<T> found = determineGenericClassParameterImpl(clazz, rawClass, paramIndex);
        if (found == null) {
            throw new AssertionError(clazz.getSimpleName() + " expected to extend " + rawClass.getSimpleName() + "<...>");
        }
        return found;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> determineGenericClassParameterImpl(
            final Class<?> clazz,
            final Class<?> rawClass,
            final int paramIndex) {
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
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> Enum<E> asEnumValue(final Class<?> type, final Object value) {
        return Enum.valueOf((Class<E>) type, value.toString());
    }

    public static Role newInstance(final Class<? extends Role> clazz) {
        return unchecked(() -> accessible(clazz.getDeclaredConstructor()).newInstance());
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {

        T get() throws Exception;
    }

    /**
     * Makes the given object accessible as if it were public.
     *
     * @param accessible field or method
     * @param <T> type of accessible
     * @return the given object
     */
    private static <T extends AccessibleObject> T accessible(final T accessible) {
        try {
            accessible.setAccessible(true);
            return accessible;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Catches checked exceptions and wraps these into an unchecked RuntimeException.
     * <p>
     * Rationale: Checked exceptions are a controversial Java feature to begin with.
     * They often mix error handling code into the normal flow of domain rules
     * or other technical aspects which is not only hard to read but also violates
     * the Single Responsibility Principle. Often this is even worse for expressions
     * than it is for statements.
     * </p>
     *
     * @param expression an expresion which returns a T and may throw a checked exception
     * @param <T> the result type of the expression
     * @return the result of the expression
     * @throws RuntimeException which wraps a checked exception thrown by the expression
     */
    public static <T> T unchecked(final ThrowingSupplier<T> expression) {
        try {
            return expression.get();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calling a method on a potentially null object. Similar to the ?: operator in Kotlin.
     *
     * @param source some object of type T
     * @param f some function mapping T to R
     * @param <T> the source type
     * @param <R> the result type
     * @return the result of f if source is not null, null otherwise
     */
    public static <T, R> R of(T source, Function<T, R> f) {
        return Optional.ofNullable(source).map(f).orElse(null);
    }

    /**
     * Forces the initialization of the given class, this means, static initialization takes place.
     *
     * If the class is already initialized, this methods does nothing.
     *
     * @param clazz the class to be initialized
     * @return the initialized class
     * 
     */
    public static <T> Class<T> initialize(Class<T> clazz) {
        try {
            Class.forName(clazz.getName(), true, clazz.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e); // Can't happen
        }
        return clazz;
    }
}
