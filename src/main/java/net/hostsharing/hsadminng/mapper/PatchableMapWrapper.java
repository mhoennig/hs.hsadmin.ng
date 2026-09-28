package net.hostsharing.hsadminng.mapper;

import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.ImmutablePair;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static java.util.Optional.ofNullable;

/** This class wraps another (usually persistent) map and
 * supports applying `PatchMap` as well as a toString method with stable entry order.
 */
public class PatchableMapWrapper<T> implements Map<String, T> {

    private static final ObjectMapper jsonWriter = JsonMapper.builder()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, SerializationFeature.INDENT_OUTPUT)
            .build();

    private final Map<String, T> delegate;
    private final Set<String> patched = new HashSet<>();

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

    public static <T> PatchableMapWrapper<T> of(final  Map<String, T> delegate) {
        return new PatchableMapWrapper<T>(delegate);
    }

    @NotNull
    public static <E> ImmutablePair<String, E> entry(final String key, final E value) {
        return new ImmutablePair<>(key, value);
    }

    public void assign(final Map<String, T> entries) {
        if (entries != null ) {
            delegate.clear();
            // entry by entry, so that a POSTed value is treated exactly like a PATCHed one
            entries.forEach(this::patch);
            patched.clear();
        }
    }

    public void patch(final Map<String, T> patch) {
        patch.forEach(this::patch);
    }

    private void patch(final String key, final T value) {
        if (value == null) {
            remove(key);
        } else {
            put(key, value);
        }
    }

    public boolean isPatched(final String propertyName) {
        return patched.contains(propertyName);
    }

    @SneakyThrows
    public String toString() {
        return jsonWriter.writeValueAsString(delegate);
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
    // a value loaded from jsonb arrives as ArrayList, just like a freshly deserialized one
    public T get(final Object key) {
        return toStringArrayIfList(delegate.get(key));
    }

    @Override
    public T put(final String key, final T value) {
        if (!Objects.equals(value, delegate.get(key))) {
            patched.add(key);
        }
        return delegate.put(key, fixValueType(key, value));
    }

    @Override
    public T remove(final Object key) {
        if (delegate.containsKey(key.toString())) {
            patched.add(key.toString());
        }
        return delegate.remove(key);
    }

    @Override
    public void putAll(final @NotNull Map<? extends String, ? extends T> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        patched.addAll(delegate.keySet());
        delegate.clear();
    }

    @Override
    @NotNull
    public Set<String> keySet() {
        return delegate.keySet();
    }

    @Override
    @NotNull
    public Collection<T> values() {
        return delegate.values();
    }

    @Override
    @NotNull
    public Set<Entry<String, T>> entrySet() {
        return delegate.entrySet();
    }

    // only requests can be rejected, thus the null-check does not belong into the read path
    private T fixValueType(final String key, final T value) {
        if (value instanceof ArrayList<?> arrayListValue) {
            IntStream.range(0, arrayListValue.size())
                    .filter(index -> arrayListValue.get(index) == null)
                    .findFirst()
                    .ifPresent(index -> {
                        throw new ValidationException("'" + key + "[" + index + "]' must not be null");
                    });
        }
        return toStringArrayIfList(value);
    }

    private T toStringArrayIfList(final T value) {
        if (value instanceof ArrayList<?> arrayListValue) {
            // Jackson deserialization creates ArrayList for JSON arrays, but we need a String[].
            // Jackson could be configured to create Object[], but that does not help.
            final var convertedValue = arrayListValue.stream()
                    .map(element -> element == null ? null : element.toString())
                    .toArray(String[]::new);
            //noinspection unchecked
            return (T) convertedValue;
        }
        return value;
    }
}
