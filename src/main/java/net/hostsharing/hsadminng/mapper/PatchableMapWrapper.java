package net.hostsharing.hsadminng.mapper;

import org.apache.commons.lang3.tuple.ImmutablePair;

import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.joining;

/** This class wraps another (usually persistent) map and
 * supports applying `PatchMap` as well as a toString method with stable entry order.
 */
public class PatchableMapWrapper implements Map<String, Object> {

    private final Map<String, Object> delegate;

    public PatchableMapWrapper(final Map<String, Object> map) {
        delegate = map;
    }

    @NotNull
    public static ImmutablePair<String, Object> entry(final String key, final Object value) {
        return new ImmutablePair<>(key, value);
    }

    public void assign(final Map<String, Object> entries) {
        delegate.clear();
        delegate.putAll(entries);
    }

    public void patch(final Map<String, Object> patch) {
        patch.forEach((key, value) -> {
            if (value == null) {
                remove(key);
            } else {
                put(key, value);
            }
        });
    }

    public String toString() {
        return "{ "
                + (
                    keySet().stream().sorted()
                            .map(k -> k + ": " + get(k)))
                            .collect(joining(", ")
                )
                + " }";
    }

    // --- below just delegating methods --------------------------------

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public Object get(final Object key) {
        return delegate.get(key);
    }

    @Override
    public Object put(final String key, final Object value) {
        return delegate.put(key, value);
    }

    @Override
    public Object remove(final Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(final Map<? extends String, ?> m) {
        delegate.putAll(m);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<String> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<Object> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return delegate.entrySet();
    }
}
