package net.hostsharing.hsadminng.hs.validation;

import lombok.Setter;
import net.hostsharing.hsadminng.mapper.Array;
import org.apache.commons.lang3.Validate;

import java.util.List;

@Setter
public class IntegerProperty<P extends IntegerProperty<P>> extends ValidatableProperty<P, Integer> {

    private final static String[] KEY_ORDER = Array.join(
            ValidatableProperty.KEY_ORDER_HEAD,
            Array.of("unit", "min", "minFrom", "max", "maxFrom", "step"),
            ValidatableProperty.KEY_ORDER_TAIL);

    private String unit;
    private Integer min;
    private String minFrom;
    private Integer max;
    private String maxFrom;
    private Integer factor;
    private Integer step;

    public static IntegerProperty<?> integerProperty(final String propertyName) {
        return new IntegerProperty<>(propertyName);
    }

    private IntegerProperty(final String propertyName) {
        super(Integer.class, propertyName, KEY_ORDER);
    }

    @Override
    public void deferredInit(final ValidatableProperty<?, ?>[] allProperties) {
        Validate.isTrue(min == null || minFrom == null, "min and minFrom are exclusive, but both are given");
        Validate.isTrue(max == null || maxFrom == null, "max and maxFrom are exclusive, but both are given");
    }

    public P minFrom(final String propertyName) {
        minFrom = propertyName;
        return self();
    }

    public P maxFrom(final String propertyName) {
        maxFrom = propertyName;
        return self();
    }

    public P withFactor(final int factor) {
        this.factor = factor;
        return self();
    }

    @Override
    public String unit() {
        return unit;
    }

    public Integer min() {
        return min;
    }

    public Integer max() {
        return max;
    }

    @Override
    protected void validate(final List<String> result, final Integer propValue, final PropertiesProvider propProvider) {
        validateMin(result, propertyName, propValue, min);
        validateMax(result, propertyName, propValue, max);
        if (step != null && propValue % step != 0) {
            result.add(propertyName + "' is expected to be multiple of " + step + " but is " + propValue);
        }
        if (minFrom != null) {
            validateMin(result, propertyName, propValue, propProvider.getContextValue(minFrom, Integer.class) * ((factor != null) ? factor : 1));
        }
        if (maxFrom != null) {
            validateMax(result, propertyName, propValue, propProvider.getContextValue(maxFrom, Integer.class, 0) * ((factor != null) ? factor : 1));
        }
    }

    @Override
    protected String simpleTypeName() {
        return "integer";
    }

    private static void validateMin(final List<String> result, final String propertyName, final Integer propValue, final Integer min) {
        if (min != null && propValue < min) {
            result.add(propertyName + "' is expected to be at least " + min + " but is " + propValue);
        }
    }

    private static void validateMax(final List<String> result, final String propertyName, final Integer propValue, final Integer max) {
        if (max != null && propValue > max) {
            result.add(propertyName + "' is expected to be at most " + max + " but is " + propValue);
        }
    }
}
