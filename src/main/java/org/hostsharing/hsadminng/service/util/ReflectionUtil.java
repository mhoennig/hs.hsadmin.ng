package org.hostsharing.hsadminng.service.util;

import java.util.function.Supplier;

public class ReflectionUtil {

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
