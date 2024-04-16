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
public class PatchMap extends TreeMap<String, Object> {

    public PatchMap(final ImmutablePair<String, Object>[] entries) {
        stream(entries).forEach(r -> put(r.getKey(), r.getValue()));
    }

    @SafeVarargs
    public static Map<String, Object> patchMap(final ImmutablePair<String, Object>... entries) {
        return new PatchMap(entries);
    }

    @NotNull
    public static ImmutablePair<String, Object> entry(final String key, final Object value) {
        return new ImmutablePair<>(key, value);
    }
}
