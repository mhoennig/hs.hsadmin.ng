package net.hostsharing.hsadminng.hs.validation;

import lombok.Setter;
import net.hostsharing.hsadminng.mapper.Array;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Setter
public class BooleanProperty extends ValidatableProperty<BooleanProperty, Boolean> {

    private static final String[] KEY_ORDER = Array.join(ValidatableProperty.KEY_ORDER_HEAD, ValidatableProperty.KEY_ORDER_TAIL);

    private Map.Entry<String, String> falseIf;

    private BooleanProperty(final String propertyName) {
        super(Boolean.class, propertyName, KEY_ORDER);
    }

    public static BooleanProperty booleanProperty(final String propertyName) {
        return new BooleanProperty(propertyName);
    }

    public BooleanProperty falseIf(final String refPropertyName, final String refPropertyValue) {
        this.falseIf = new AbstractMap.SimpleImmutableEntry<>(refPropertyName, refPropertyValue);
        return this;
    }

    @Override
    protected void validate(final List<String> result, final Boolean propValue, final PropertiesProvider propProvider) {
        if (falseIf != null && propValue) {
            final Object referencedValue = propProvider.directProps().get(falseIf.getKey());
            if (Objects.equals(referencedValue, falseIf.getValue())) {
                result.add(propertyName + "' is expected to be false because " +
                        falseIf.getKey() + "=" + referencedValue + " but is " + propValue);
            }
        }
    }

    @Override
    protected String simpleTypeName() {
        return "boolean";
    }
}
