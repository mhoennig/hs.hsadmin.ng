package net.hostsharing.hsadminng.hs.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public abstract class HsPropertyValidator<T> {

    final Class<T> type;
    final String propertyName;
    private Boolean required;

    public static <K, V> Map.Entry<K, V> defType(K k, V v) {
        return new SimpleImmutableEntry<>(k, v);
    }

    public HsPropertyValidator<T> required() {
        required = Boolean.TRUE;
        return this;
    }

    public HsPropertyValidator<T> optional() {
        required = Boolean.FALSE;
        return this;
    }

    public final List<String> validate(final String propertiesName, final Map<String, Object> props) {
        final var result = new ArrayList<String>();
        final var propValue = props.get(propertyName);
        if (propValue == null) {
            if (required) {
                result.add("'"+propertiesName+"." + propertyName + "' is required but missing");
            }
        }
        if (propValue != null){
            if ( type.isInstance(propValue)) {
                //noinspection unchecked
                validate(result, propertiesName, (T) propValue, props);
            } else {
                result.add("'"+propertiesName+"." + propertyName + "' is expected to be of type " + type + ", " +
                        "but is of type '" + propValue.getClass().getSimpleName() + "'");
            }
        }
        return result;
    }

    protected abstract void validate(final ArrayList<String> result, final String propertiesName, final T propValue, final Map<String, Object> props);

    public void verifyConsistency(final Map.Entry<? extends Enum<?>, ?> typeDef) {
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
