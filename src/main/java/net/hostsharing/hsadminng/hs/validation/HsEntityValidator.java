package net.hostsharing.hsadminng.hs.validation;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;

public abstract class HsEntityValidator<E> {

    public final ValidatableProperty<?>[] propertyValidators;

    public HsEntityValidator(final ValidatableProperty<?>... validators) {
        propertyValidators = validators;
        stream(propertyValidators).forEach(p -> p.deferredInit(propertyValidators));
    }

    protected static List<String> enrich(final String prefix, final List<String> messages) {
        return messages.stream()
                // TODO:refa: this is a bit hacky, I need to find the right place to add the prefix
                .map(message -> message.startsWith("'") ? message : ("'" + prefix + "." + message))
                .toList();
    }

    protected static String prefix(final String... parts) {
        return String.join(".", parts);
    }

    public abstract List<String> validate(final E entity);

    public final List<Map<String, Object>> properties() {
        return Arrays.stream(propertyValidators)
                .map(ValidatableProperty::toOrderedMap)
                .toList();
    }

    protected ArrayList<String> validateProperties(final Map<String, Object> properties) {
        final var result = new ArrayList<String>();
        properties.keySet().forEach( givenPropName -> {
            if (stream(propertyValidators).map(pv -> pv.propertyName).noneMatch(propName -> propName.equals(givenPropName))) {
                result.add(givenPropName + "' is not expected but is set to '" + properties.get(givenPropName) + "'");
            }
        });
        stream(propertyValidators).forEach(pv -> {
            result.addAll(pv.validate(properties));
        });
        return result;
    }

    @SafeVarargs
    protected static List<String> sequentiallyValidate(final Supplier<List<String>>... validators) {
        return new ArrayList<>(stream(validators)
                .map(Supplier::get)
                .filter(violations -> !violations.isEmpty())
                .findFirst()
                .orElse(emptyList()));
    }

    protected static Integer getIntegerValueWithDefault0(final ValidatableProperty<?> prop, final Map<String, Object> propValues) {
        final var value = prop.getValue(propValues);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value == null) {
            return 0;
        }
        throw new IllegalArgumentException(prop.propertyName + " Integer value expected, but got " + value);
    }

    protected static Integer toIntegerWithDefault0(final Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value == null) {
            return 0;
        }
        throw new IllegalArgumentException("Integer value (or null) expected, but got " + value);
    }
}
