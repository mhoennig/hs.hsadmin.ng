package net.hostsharing.hsadminng.hs.validation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.stream;

public class HsEntityValidator<E extends Validatable<E, T>, T extends Enum<T>> {

    public final HsPropertyValidator<?>[] propertyValidators;

    public HsEntityValidator(final HsPropertyValidator<?>... validators) {
        propertyValidators = validators;
    }

    public List<String> validate(final E assetEntity) {
        final var result = new ArrayList<String>();
        assetEntity.getProperties().keySet().forEach( givenPropName -> {
            if (stream(propertyValidators).map(pv -> pv.propertyName).noneMatch(propName -> propName.equals(givenPropName))) {
                result.add("'"+assetEntity.getPropertiesName()+"." + givenPropName + "' is not expected but is set to '" +assetEntity.getProperties().get(givenPropName) + "'");
            }
        });
        stream(propertyValidators).forEach(pv -> {
          result.addAll(pv.validate(assetEntity.getPropertiesName(), assetEntity.getProperties()));
        });
        return result;
    }

    public List<Map<String, Object>> properties() {
        final var mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return Arrays.stream(propertyValidators)
                .map(propertyValidator -> propertyValidator.toMap(mapper))
                .map(HsEntityValidator::asKeyValueMap)
                .toList();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Map<String, Object> asKeyValueMap(final Map map) {
        return (Map<String, Object>) map;
    }

}
