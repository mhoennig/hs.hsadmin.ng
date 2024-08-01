package net.hostsharing.hsadminng.mapper;

import org.apache.commons.lang3.tuple.ImmutablePair;

import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

/** This class wraps another (usually persistent) map and
 * supports applying `PatchMap` as well as a toString method with stable entry order.
 */
public class PatchableMapWrapper<T> implements Map<String, T> {

    private final Map<String, T> delegate;

    private PatchableMapWrapper(final Map<String, T> map) {
        delegate = map;
    }

    public static <T> PatchableMapWrapper<T> of(final PatchableMapWrapper<T> currentWrapper, final Consumer<PatchableMapWrapper<T>> setWrapper, final  Map<String, T> target) {
        return ofNullable(currentWrapper).orElseGet(() -> {
            final var newWrapper = new PatchableMapWrapper<T>(target);
            setWrapper.accept(newWrapper);
            return newWrapper;
        });
    }

    @NotNull
    public static <E> ImmutablePair<String, E> entry(final String key, final E value) {
        return new ImmutablePair<>(key, value);
    }

    public void assign(final Map<String, T> entries) {
        if (entries != null ) {
            delegate.clear();
            delegate.putAll(entries);
        }
    }

    public void patch(final Map<String, T> patch) {
        patch.forEach((key, value) -> {
            if (value == null) {
                remove(key);
            } else {
                put(key, value);
            }
        });
    }

    public String toString() {
        return "{\n"
                + (
                    keySet().stream().sorted()
                            .map(k -> "    \"" + k + "\": " + formatted(get(k))))
                            .collect(joining(",\n")
                )
                + "\n}\n";
    }

    private Object formatted(final Object value) {
        if ( value == null || value instanceof Number || value instanceof Boolean ) {
            return value;
        }
        if ( value.getClass().isArray() ) {
            return "\"" + Arrays.toString( (Object[]) value) + "\"";
        }
        return "\"" + value + "\"";
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
    public T get(final Object key) {
        return delegate.get(key);
    }

    @Override
    public T put(final String key, final T value) {
        return delegate.put(key, value);
    }

    @Override
    public T remove(final Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(final @NotNull Map<? extends String, ? extends T> m) {
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
    public Collection<T> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<String, T>> entrySet() {
        return delegate.entrySet();
    }
}
