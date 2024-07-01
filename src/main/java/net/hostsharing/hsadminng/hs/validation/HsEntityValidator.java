package net.hostsharing.hsadminng.hs.validation;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;

// TODO.refa: rename to HsEntityProcessor, also subclasses
public abstract class HsEntityValidator<E extends PropertiesProvider> {

    public final ValidatableProperty<?, ?>[] propertyValidators;

    public HsEntityValidator(final ValidatableProperty<?, ?>... validators) {
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

    public abstract List<String> validateEntity(final E entity);
    public abstract List<String> validateContext(final E entity);

    public final List<Map<String, Object>> properties() {
        return Arrays.stream(propertyValidators)
                .map(ValidatableProperty::toOrderedMap)
                .toList();
    }

    protected ArrayList<String> validateProperties(final PropertiesProvider propsProvider) {
        final var result = new ArrayList<String>();

        // verify that all actually given properties are specified
        final var properties = propsProvider.directProps();
        properties.keySet().forEach( givenPropName -> {
            if (stream(propertyValidators).map(pv -> pv.propertyName).noneMatch(propName -> propName.equals(givenPropName))) {
                result.add(givenPropName + "' is not expected but is set to '" + properties.get(givenPropName) + "'");
            }
        });

        // run all property validators
        stream(propertyValidators).forEach(pv -> {
            result.addAll(pv.validate(propsProvider));
        });

        return result;
    }

    @SafeVarargs
    public static List<String> sequentiallyValidate(final Supplier<List<String>>... validators) {
        return new ArrayList<>(stream(validators)
                .map(Supplier::get)
                .filter(violations -> !violations.isEmpty())
                .findFirst()
                .orElse(emptyList()));
    }

    protected static Integer getIntegerValueWithDefault0(final ValidatableProperty<?, ?> prop, final Map<String, Object> propValues) {
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

    public void prepareProperties(final E entity) {
        stream(propertyValidators).forEach(p -> {
            if ( p.isWriteOnly() && p.isComputed()) {
                entity.directProps().put(p.propertyName, p.compute(entity));
            }
        });
    }

    public Map<String, Object> revampProperties(final E entity, final Map<String, Object> config) {
        final var copy = new HashMap<>(config);
        stream(propertyValidators).forEach(p -> {
            if (p.isWriteOnly()) {
                copy.remove(p.propertyName);
            } else if (p.isReadOnly() && p.isComputed()) {
                copy.put(p.propertyName, p.compute(entity));
            }
        });
        return copy;
    }
}
