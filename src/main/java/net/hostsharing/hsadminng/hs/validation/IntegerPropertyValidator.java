package net.hostsharing.hsadminng.hs.validation;

import lombok.Setter;

import java.util.ArrayList;
import java.util.Map;

@Setter
public class IntegerPropertyValidator extends HsPropertyValidator<Integer> {

    private String unit;
    private Integer min;
    private Integer max;
    private Integer step;

    public static IntegerPropertyValidator integerProperty(final String propertyName) {
        return new IntegerPropertyValidator(propertyName);
    }

    private IntegerPropertyValidator(final String propertyName) {
        super(Integer.class, propertyName);
    }


    @Override
    protected void validate(final ArrayList<String> result, final String propertiesName, final Integer propValue, final Map<String, Object> props) {
        if (min != null && propValue < min) {
            result.add("'"+propertiesName+"." + propertyName + "' is expected to be >= " + min + " but is " + propValue);
        }
        if (max != null && propValue > max) {
            result.add("'"+propertiesName+"." + propertyName + "' is expected to be <= " + max + " but is " + propValue);
        }
        if (step != null && propValue % step != 0) {
            result.add("'"+propertiesName+"." + propertyName + "' is expected to be multiple of " + step + " but is " + propValue);
        }
    }

    @Override
    protected String simpleTypeName() {
        return "integer";
    }
}
