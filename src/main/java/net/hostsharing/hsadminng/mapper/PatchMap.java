package net.hostsharing.hsadminng.mapper;

import org.apache.commons.lang3.tuple.ImmutablePair;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Arrays.stream;

/**
 * This is a map which can take key-value-pairs where the value can be null
 * thus JSON nullable object structures from HTTP PATCH can be represented.
 */
public class PatchMap<T> extends TreeMap<String, T> {

    public PatchMap(final ImmutablePair<String, T>[] entries) {
        stream(entries).forEach(r -> put(r.getKey(), r.getValue()));
    }

    @SafeVarargs
    public static <T> Map<String, T> patchMap(final ImmutablePair<String, Object>... entries) {
        return new PatchMap(entries);
    }

    @NotNull
    public static <T> ImmutablePair<String, T> entry(final String key, final T value) {
        return new ImmutablePair<>(key, value);
    }
}
