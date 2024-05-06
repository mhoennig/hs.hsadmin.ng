package net.hostsharing.hsadminng.hs.hosting.asset.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
public abstract class HsHostingAssetPropertyValidator<T> {

    final Class<T> type;
    final String propertyName;
    private Boolean required;

    public static <K, V> Map.Entry<K, V> defType(K k, V v) {
        return new SimpleImmutableEntry<>(k, v);
    }

    public HsHostingAssetPropertyValidator<T> required() {
        required = Boolean.TRUE;
        return this;
    }

    public HsHostingAssetPropertyValidator<T> optional() {
        required = Boolean.FALSE;
        return this;
    }

    public final List<String> validate(final Map<String, Object> props) {
        final var result = new ArrayList<String>();
        final var propValue = props.get(propertyName);
        if (propValue == null) {
            if (required) {
                result.add("'" + propertyName + "' is required but missing");
            }
        }
        if (propValue != null){
            if ( type.isInstance(propValue)) {
                //noinspection unchecked
                validate(result, (T) propValue, props);
            } else {
                result.add("'" + propertyName + "' is expected to be of type " + type + ", " +
                        "but is of type '" + propValue.getClass().getSimpleName() + "'");
            }
        }
        return result;
    }

    protected abstract void validate(final ArrayList<String> result, final T propValue, final Map<String, Object> props);

    public void verifyConsistency(final Map.Entry<HsHostingAssetType, HsHostingAssetValidator> typeDef) {
        if (required == null ) {
            throw new IllegalStateException(typeDef.getKey() + "[" + propertyName + "] not fully initialized, please call either .required() or .optional()" );
        }
    }

    public Map<String, Object> toMap(final ObjectMapper mapper) {
        final Map<String, Object> map = mapper.convertValue(this, Map.class);
        map.put("type", simpleTypeName());
        return map;
    }

    protected abstract String simpleTypeName();
}

@Setter
class IntegerPropertyValidator extends HsHostingAssetPropertyValidator<Integer>{

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
    protected void validate(final ArrayList<String> result, final Integer propValue, final Map<String, Object> props) {
        if (min != null && propValue < min) {
            result.add("'" + propertyName + "' is expected to be >= " + min + " but is " + propValue);
        }
        if (max != null && propValue > max) {
            result.add("'" + propertyName + "' is expected to be <= " + max + " but is " + propValue);
        }
        if (step != null && propValue % step != 0) {
            result.add("'" + propertyName + "' is expected to be multiple of " + step + " but is " + propValue);
        }
    }

    @Override
    protected String simpleTypeName() {
        return "integer";
    }
}

@Setter
class EnumPropertyValidator extends HsHostingAssetPropertyValidator<String> {

    private String[] values;

    private EnumPropertyValidator(final String propertyName) {
        super(String.class, propertyName);
    }

    public static EnumPropertyValidator enumerationProperty(final String propertyName) {
        return new EnumPropertyValidator(propertyName);
    }

    public HsHostingAssetPropertyValidator<String> values(final String... values) {
        this.values = values;
        return this;
    }

    @Override
    protected void validate(final ArrayList<String> result, final String propValue, final Map<String, Object> props) {
        if (Arrays.stream(values).noneMatch(v -> v.equals(propValue))) {
            result.add("'" + propertyName + "' is expected to be one of " + Arrays.toString(values) + " but is '" + propValue + "'");
        }
    }

    @Override
    protected String simpleTypeName() {
        return "enumeration";
    }
}

@Setter
class BooleanPropertyValidator extends HsHostingAssetPropertyValidator<Boolean> {

    private Map.Entry<String, String> falseIf;

    private BooleanPropertyValidator(final String propertyName) {
        super(Boolean.class, propertyName);
    }

    public static BooleanPropertyValidator booleanProperty(final String propertyName) {
        return new BooleanPropertyValidator(propertyName);
    }

    HsHostingAssetPropertyValidator<Boolean> falseIf(final String refPropertyName, final String refPropertyValue) {
        this.falseIf = new SimpleImmutableEntry<>(refPropertyName, refPropertyValue);
        return this;
    }

    @Override
    protected void validate(final ArrayList<String> result, final Boolean propValue, final Map<String, Object> props) {
        if (falseIf != null && !Objects.equals(props.get(falseIf.getKey()), falseIf.getValue())) {
            if (propValue) {
                result.add("'" + propertyName + "' is expected to be false because " +
                        falseIf.getKey()+ "=" + falseIf.getValue() + " but is " + propValue);
            }
        }
    }

    @Override
    protected String simpleTypeName() {
        return "boolean";
    }
}
