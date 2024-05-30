package net.hostsharing.hsadminng.hs.validation;

import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

@Setter
public class EnumerationPropertyValidator extends HsPropertyValidator<String> {

    private String[] values;

    private EnumerationPropertyValidator(final String propertyName) {
        super(String.class, propertyName);
    }

    public static EnumerationPropertyValidator enumerationProperty(final String propertyName) {
        return new EnumerationPropertyValidator(propertyName);
    }

    public HsPropertyValidator<String> values(final String... values) {
        this.values = values;
        return this;
    }

    @Override
    protected void validate(final ArrayList<String> result, final String propertiesName, final String propValue, final Map<String, Object> props) {
        if (Arrays.stream(values).noneMatch(v -> v.equals(propValue))) {
            result.add("'"+propertiesName+"." + propertyName + "' is expected to be one of " + Arrays.toString(values) + " but is '" + propValue + "'");
        }
    }

    @Override
    protected String simpleTypeName() {
        return "enumeration";
    }
}
