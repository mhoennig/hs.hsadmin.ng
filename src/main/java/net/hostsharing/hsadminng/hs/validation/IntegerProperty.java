package net.hostsharing.hsadminng.hs.validation;

import lombok.Setter;
import net.hostsharing.hsadminng.mapper.Array;

import java.util.ArrayList;
import java.util.Map;

@Setter
public class IntegerProperty extends ValidatableProperty<Integer> {

    private final static String[] KEY_ORDER = Array.join(
            ValidatableProperty.KEY_ORDER_HEAD,
            Array.of("unit", "min", "max", "step"),
            ValidatableProperty.KEY_ORDER_TAIL);

    private String unit;
    private Integer min;
    private Integer max;
    private Integer step;

    public static IntegerProperty integerProperty(final String propertyName) {
        return new IntegerProperty(propertyName);
    }

    private IntegerProperty(final String propertyName) {
        super(Integer.class, propertyName, KEY_ORDER);
    }

    @Override
    public String unit() {
        return unit;
    }

    public Integer max() {
        return max;
    }

    @Override
    protected void validate(final ArrayList<String> result, final Integer propValue, final Map<String, Object> props) {
        if (min != null && propValue < min) {
            result.add(propertyName + "' is expected to be >= " + min + " but is " + propValue);
        }
        if (max != null && propValue > max) {
            result.add(propertyName + "' is expected to be <= " + max + " but is " + propValue);
        }
        if (step != null && propValue % step != 0) {
            result.add(propertyName + "' is expected to be multiple of " + step + " but is " + propValue);
        }
    }

    @Override
    protected String simpleTypeName() {
        return "integer";
    }
}
