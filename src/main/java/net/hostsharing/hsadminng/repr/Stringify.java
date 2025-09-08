package net.hostsharing.hsadminng.repr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import net.hostsharing.hsadminng.errors.DisplayAs;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

public final class Stringify<B> {

    private final String name;
    private Function<? extends B, ?> idProp;
    private final List<Property<B, ?>> props = new ArrayList<>();
    private String separator = ", ";
    private Boolean quotedValues = null;

    public static <B> Stringify<B> stringify(final Class<B> clazz, final String name) {
        return new Stringify<>(clazz, name);
    }

    public static <B> Stringify<B> stringify(final Class<B> clazz) {
        return new Stringify<>(clazz, null);
    }

    private Stringify(final Class<B> clazz, final String name) {
        if (name != null) {
            this.name = name;
        } else {
            final var displayName = clazz.getAnnotation(DisplayAs.class);
            if (displayName != null) {
                this.name = displayName.value();
            } else {
                this.name = clazz.getSimpleName();
            }
        }
    }

    public <V> Stringify<B> withIdProp(final Function<? extends B, V> getter) {
        idProp = getter;
        return this;
    }

    public <V> Stringify<B> withProp(final String propName, final Function<B, V> getter) {
        props.add(new Property<>(propName, getter));
        return this;
    }

    public <V> Stringify<B> withProp(final Function<B, V> getter) {
        props.add(new Property<>(null, getter));
        return this;
    }

    public <V> Stringify<B> withProp(final Function<B, V> getter, final Function<V, ?> mapper) {
        props.add(new Property<>(null, getter, mapper));
        return this;
    }

    public String apply(@NotNull B object) {
        final var propValues = props.stream()
                .map(prop -> new PropertyValue<>(object, prop))
                .filter(PropertyValue::notNullAndNotEmpty)
                .map(propVal -> propName(propVal, "=") + optionallyQuoted(propVal))
                .collect(Collectors.joining(separator));
        return idProp != null
            ? name + "(" + idProp.apply(cast(object)) + ": " + propValues + ")"
            : name + "(" + propValues + ")";
    }

    public Stringify<B> withSeparator(final String separator) {
        this.separator = separator;
        return this;
    }

    private <V> String propName(final PropertyValue<B, V> propVal, final String delimiter) {
        return ofNullable(propVal.prop.name).map(v -> v + delimiter).orElse("");
    }

    private <B, V> String optionallyQuoted(final PropertyValue<B, V> propVal) {
        if (quotedValues == null)
            return quotableValueType(propVal.getValue())
                    ? ("'" + propVal.stringValue + "'")
                    : propVal.stringValue;
        return quotedValues
                ? ("'" + propVal.stringValue + "'")
                : propVal.stringValue;
    }

    private <V> boolean quotableValueType(final V rawValue) {
        return !(rawValue instanceof Enum) &&
                !(rawValue instanceof Symbol) &&
                !(rawValue instanceof Number) &&
                !(rawValue instanceof Boolean);
    }

    /**
     *  Specifies whether the values should be quoted (true) or not (false).
     *
     *  If not specified, Enum, Symbol, Number and Boolean values are not quoted;
     *  other value types are quoted.
     *
     * @param quotedValues
     * @return
     */
    public Stringify<B> quotedValues(final boolean quotedValues) {
        this.quotedValues = quotedValues;
        return this;
    }

    private <T> T cast(final Object object) {
        //noinspection unchecked
        return (T)object;
    }

    @Value
    @AllArgsConstructor
    private class Property<B, V> {
        String name;
        Function<B, V> getter;
        Function<V, ?> mapper; // FIXME: better generics?

        Property(String name, Function<B, V> getter) {
            this(name, getter, v -> v);
        }

        Object getValue(final B object) {
            return ofNullable(getter.apply(object))
                .map(mapper)
                .orElse(null);
        }
    }

    @Getter
    private class PropertyValue<B, V> {
        private Property<B, V> prop;
        private V value;
        private String stringValue;

        @SuppressWarnings("unchecked")
        PropertyValue(final B object, final Property<B, ?> prop) {
            this.prop = (Property<B, V>) prop;
            this.value = (V) this.prop.getValue(object);
            this.stringValue = this.value instanceof Stringifyable s
                    ? s.toShortString()
                    : Objects.toString(this.value);
        }

        boolean notNullAndNotEmpty() {
            return value != null &&
                    (!(value instanceof Collection<?> c) || !c.isEmpty()) &&
                    (!(value instanceof Map<?,?> m) || !m.isEmpty()) &&
                    (!(value instanceof String s) || !s.isEmpty());
        }
    }
}
