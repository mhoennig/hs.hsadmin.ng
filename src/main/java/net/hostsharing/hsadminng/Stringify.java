package net.hostsharing.hsadminng;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

// TODO.refa: use this instead of toDisplayName everywhere and add JavaDoc
public class Stringify<B> {

    private final Class<B> clazz;
    private final String name;
    private final List<Property<B>> props = new ArrayList<>();

    public static <B> Stringify<B> stringify(final Class<B> clazz, final String name) {
        return new Stringify<B>(clazz, name);
    }

    public static <B> Stringify<B> stringify(final Class<B> clazz) {
        return new Stringify<B>(clazz, null);
    }

    private Stringify(final Class<B> clazz, final String name) {
        this.clazz = clazz;
        this.name = name;
    }

    public Stringify<B> withProp(final String propName, final Function<B, ?> getter) {
        props.add(new Property<B>(propName, getter));
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
                .map(propVal -> propVal.prop.name + "=" + optionallyQuoted(propVal))
                .collect(Collectors.joining(", "));
        return (name != null ? name : object.getClass().getSimpleName()) + "(" + propValues + ")";
    }

    private String optionallyQuoted(final PropertyValue<B> propVal) {
        return (propVal.rawValue instanceof Number) || (propVal.rawValue instanceof Boolean)
                ? propVal.value
                : "'" + propVal.value + "'";
    }

    private record Property<B>(String name, Function<B, ?> getter) {}

    private record PropertyValue<B>(Property<B> prop, Object rawValue, String value) {
        static <B> PropertyValue<B> of(Property<B> prop, Object rawValue) {
            return rawValue != null ? new PropertyValue<>(prop, rawValue, rawValue.toString()) : null;
        }
    }
}
