package net.hostsharing.hsadminng.repr;

import net.hostsharing.hsadminng.errors.DisplayAs;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;

public final class Stringify<B> {

    private final String name;
    private Function<? extends B, ?> idProp;
    private final List<Property<B>> props = new ArrayList<>();
    private String separator = ", ";
    private Boolean quotedValues = null;

    public static <B> Stringify<B> stringify(final Class<B> clazz, final String name) {
        return new Stringify<>(clazz, name);
    }

    public static <B> Stringify<B> stringify(final Class<B> clazz) {
        return new Stringify<>(clazz, null);
    }

    public <T extends B> Stringify<T> using(final Class<T> subClass) {
        //noinspection unchecked
        final var stringify = new Stringify<T>(subClass, null)
                .withIdProp(cast(idProp))
                .withProps(cast(props))
                .withSeparator(separator);
        if (quotedValues != null) {
            stringify.quotedValues(quotedValues);
        }
        return stringify;
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

    public Stringify<B> withIdProp(final Function<? extends B, ?> getter) {
        idProp = getter;
        return this;
    }

    public Stringify<B> withProp(final String propName, final Function<B, ?> getter) {
        props.add(new Property<>(propName, getter));
        return this;
    }

    public Stringify<B> withProp(final Function<B, ?> getter) {
        props.add(new Property<>(null, getter));
        return this;
    }

    private Stringify<B> withProps(final List<Property<B>> props) {
        this.props.addAll(props);
        return this;
    }

    public String apply(@NotNull B object) {
        final var propValues = props.stream()
                .map(prop -> PropertyValue.of(prop, prop.getter.apply(object)))
                .filter(Objects::nonNull)
                .filter(PropertyValue::nonEmpty)
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

    private String propName(final PropertyValue<B> propVal, final String delimiter) {
        return ofNullable(propVal.prop.name).map(v -> v + delimiter).orElse("");
    }

    private String optionallyQuoted(final PropertyValue<B> propVal) {
        if (quotedValues == null)
            return quotedQuotedValueType(propVal)
                    ? ("'" + propVal.value + "'")
                    : propVal.value;
        return TRUE == quotedValues
                ? ("'" + propVal.value + "'")
                : propVal.value;
    }

    private static <B> boolean quotedQuotedValueType(final PropertyValue<B> propVal) {
        return !(propVal.rawValue instanceof Number || propVal.rawValue instanceof Boolean);
    }

    public Stringify<B> quotedValues(final boolean quotedValues) {
        this.quotedValues = quotedValues;
        return this;
    }

    private <T> T cast(final Object object) {
        //noinspection unchecked
        return (T)object;
    }

    private record Property<B>(String name, Function<B, ?> getter) {}

    private record PropertyValue<B>(Property<B> prop, Object rawValue, String value) {

        static <B> PropertyValue<B> of(Property<B> prop, Object rawValue) {
            return rawValue != null ? new PropertyValue<>(prop, rawValue, toStringOrShortString(rawValue)) : null;
        }

        private static String toStringOrShortString(final Object rawValue) {
            return rawValue instanceof Stringifyable stringifyable ? stringifyable.toShortString() : rawValue.toString();
        }

        boolean nonEmpty() {
            return rawValue != null &&
                    (!(rawValue instanceof Collection<?> c) || !c.isEmpty()) &&
                    (!(rawValue instanceof Map<?,?> m) || !m.isEmpty()) &&
                    (!(rawValue instanceof String s) || !s.isEmpty());
        }
    }
}
