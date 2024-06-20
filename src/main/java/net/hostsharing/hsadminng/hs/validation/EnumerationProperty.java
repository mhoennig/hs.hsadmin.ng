package net.hostsharing.hsadminng.hs.validation;

import lombok.Setter;
import net.hostsharing.hsadminng.mapper.Array;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static java.util.Arrays.stream;

@Setter
public class EnumerationProperty extends ValidatableProperty<String> {

    private static final String[] KEY_ORDER = Array.join(
            ValidatableProperty.KEY_ORDER_HEAD,
            Array.of("values"),
            ValidatableProperty.KEY_ORDER_TAIL);

    private String[] values;

    private EnumerationProperty(final String propertyName) {
        super(String.class, propertyName, KEY_ORDER);
    }

    public static EnumerationProperty enumerationProperty(final String propertyName) {
        return new EnumerationProperty(propertyName);
    }

    public ValidatableProperty<String> values(final String... values) {
        this.values = values;
        return this;
    }

    public void deferredInit(final ValidatableProperty<?>[] allProperties) {
        if (deferredInit != null) {
            if (this.values != null) {
                throw new IllegalStateException("property " + toString() + " already has values");
            }
            this.values = deferredInit.apply(allProperties);
        }
    }

    public ValidatableProperty<String> valuesFromProperties(final String propertyNamePrefix) {
        this.deferredInit = (ValidatableProperty<?>[] allProperties) -> stream(allProperties)
                .map(ValidatableProperty::propertyName)
                .filter(name -> name.startsWith(propertyNamePrefix))
                .map(name -> name.substring(propertyNamePrefix.length()))
                .toArray(String[]::new);
        return this;
    }

    @Override
    protected void validate(final ArrayList<String> result, final String propValue, final Map<String, Object> props) {
        if (stream(values).noneMatch(v -> v.equals(propValue))) {
            result.add(propertyName + "' is expected to be one of " + Arrays.toString(values) + " but is '" + propValue + "'");
        }
    }

    @Override
    protected String simpleTypeName() {
        return "enumeration";
    }
}
