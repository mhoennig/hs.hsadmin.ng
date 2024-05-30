package net.hostsharing.hsadminng.hs.validation;

import lombok.Setter;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

@Setter
public class BooleanPropertyValidator extends HsPropertyValidator<Boolean> {

    private Map.Entry<String, String> falseIf;

    private BooleanPropertyValidator(final String propertyName) {
        super(Boolean.class, propertyName);
    }

    public static BooleanPropertyValidator booleanProperty(final String propertyName) {
        return new BooleanPropertyValidator(propertyName);
    }

    public HsPropertyValidator<Boolean> falseIf(final String refPropertyName, final String refPropertyValue) {
        this.falseIf = new AbstractMap.SimpleImmutableEntry<>(refPropertyName, refPropertyValue);
        return this;
    }

    @Override
    protected void validate(final ArrayList<String> result, final String propertiesName, final Boolean propValue, final Map<String, Object> props) {
        if (falseIf != null && !Objects.equals(props.get(falseIf.getKey()), falseIf.getValue())) {
            if (propValue) {
                result.add("'"+propertiesName+"." + propertyName + "' is expected to be false because " +
                        propertiesName+"." + falseIf.getKey()+ "=" + falseIf.getValue() + " but is " + propValue);
            }
        }
    }

    @Override
    protected String simpleTypeName() {
        return "boolean";
    }
}
