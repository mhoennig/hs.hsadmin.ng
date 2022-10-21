package net.hostsharing.hsadminng.stringify;

import net.hostsharing.hsadminng.errors.DisplayName;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;

public final class Stringify<B> {

    private final Class<B> clazz;
    private final String name;
    private final List<Property<B>> props = new ArrayList<>();
    private String separator = ", ";
    private Boolean quotedValues = null;

    public static <B> Stringify<B> stringify(final Class<B> clazz, final String name) {
        return new Stringify<>(clazz, name);
    }

    public static <B> Stringify<B> stringify(final Class<B> clazz) {
        return new Stringify<>(clazz, null);
    }

    private Stringify(final Class<B> clazz, final String name) {
        this.clazz = clazz;
        if (name != null) {
            this.name = name;
        } else {
            final var displayName = clazz.getAnnotation(DisplayName.class);
            if (displayName != null) {
                this.name = displayName.value();
            } else {
                this.name = clazz.getSimpleName();
            }
        }
    }

    public Stringify<B> withProp(final String propName, final Function<B, ?> getter) {
        props.add(new Property<>(propName, getter));
        return this;
    }

    public Stringify<B> withProp(final Function<B, ?> getter) {
        props.add(new Property<>(null, getter));
        return this;
    }

    public String apply(@NotNull B object) {
        final var propValues = props.stream()
                .map(prop -> PropertyValue.of(prop, prop.getter.apply(object)))
                .filter(Objects::nonNull)
                .map(propVal -> {
                    if (propVal.rawValue instanceof Stringifyable stringifyable) {
                        return new PropertyValue<>(propVal.prop, propVal.rawValue, stringifyable.toShortString());
                    }
                    return propVal;
                })
                .map(propVal -> propName(propVal, "=") + optionallyQuoted(propVal))
                .collect(Collectors.joining(separator));
        return name + "(" + propValues + ")";
    }

    public Stringify<B> withSeparator(final String separator) {
        this.separator = separator;
        return this;
    }

    private String propName(final PropertyValue<B> propVal, final String delimiter) {
        return Optional.ofNullable(propVal.prop.name).map(v -> v + delimiter).orElse("");
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

    private record Property<B>(String name, Function<B, ?> getter) {}

    private record PropertyValue<B>(Property<B> prop, Object rawValue, String value) {

        static <B> PropertyValue<B> of(Property<B> prop, Object rawValue) {
            return rawValue != null ? new PropertyValue<>(prop, rawValue, rawValue.toString()) : null;
        }
    }
}
