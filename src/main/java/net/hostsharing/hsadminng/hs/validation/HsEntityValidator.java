package net.hostsharing.hsadminng.hs.validation;



import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static net.hostsharing.hsadminng.hs.validation.ValidatableProperty.ComputeMode.IN_INIT;
import static net.hostsharing.hsadminng.hs.validation.ValidatableProperty.ComputeMode.IN_PREP;
import static net.hostsharing.hsadminng.hs.validation.ValidatableProperty.ComputeMode.IN_REVAMP;

// TODO.refa: rename to HsEntityProcessor, also subclasses
public abstract class HsEntityValidator<E extends PropertiesProvider> {

    public static final ThreadLocal<EntityManager> localEntityManager = new ThreadLocal<>();

    public final ValidatableProperty<?, ?>[] propertyValidators;

    public <T extends Enum <T>> HsEntityValidator(final ValidatableProperty<?, ?>... validators) {
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

    public static <R> R doWithEntityManager(final EntityManager em, final Supplier<R> code) {
        localEntityManager.set(em);
        try {
            return code.get();
        } finally {
            localEntityManager.remove();
        }
    }

    public abstract List<String> validateEntity(final E entity);
    public abstract List<String> validateContext(final E entity);

    public final List<Map<String, Object>> properties() {
        return Arrays.stream(propertyValidators)
                .map(ValidatableProperty::toOrderedMap)
                .toList();
    }

    public final Map<String, Map<String, Object>> propertiesMap() {
        return Arrays.stream(propertyValidators)
                .map(ValidatableProperty::toOrderedMap)
                .collect(Collectors.toMap(p -> p.get("propertyName").toString(), p -> p));
    }

    /**
        Gets called before any validations take place.
        Allows to initialize fields and properties to default values.
     */
    public void preprocessEntity(final E entity) {
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

    public void prepareProperties(final EntityManager em, final E entity) {
        stream(propertyValidators).forEach(p -> {
            if (p.isComputed(IN_PREP) || p.isComputed(IN_INIT) && !entity.isLoaded() ) {
                entity.directProps().put(p.propertyName, p.compute(em, entity));
            }
        });
    }

    public Map<String, Object> revampProperties(final EntityManager em, final E entity, final Map<String, Object> config) {
        final var copy = new HashMap<>(config);
        stream(propertyValidators).forEach(p -> {
            if (p.isWriteOnly()) {
                copy.remove(p.propertyName);
            } else if (p.isComputed(IN_REVAMP)) {
                copy.put(p.propertyName, p.compute(em, entity));
            }
        });
        return copy;
    }

    protected String getPropertyValue(final PropertiesProvider entity, final String propertyName) {
        final var rawValue = entity.getDirectValue(propertyName, Object.class);
        if (rawValue != null) {
            return rawValue.toString();
        }
        return Objects.toString(propertiesMap().get(propertyName).get("defaultValue"));
    }

    protected String getPropertyValues(final PropertiesProvider entity, final String propertyName) {
        final var rawValue = entity.getDirectValue(propertyName, Object[].class);
        if (rawValue != null) {
            return stream(rawValue).map(Object::toString).collect(Collectors.joining("\n"));
        }
        return "";
    }

    public ValidatableProperty<?, ?> getProperty(final String propertyName) {
        return stream(propertyValidators).filter(pv -> pv.propertyName().equals(propertyName)).findFirst().orElse(null);
    }
}
