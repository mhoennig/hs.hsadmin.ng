package net.hostsharing.hsadminng.mapper;

import org.openapitools.jackson.nullable.JsonNullable;

import jakarta.validation.ValidationException;
import java.util.Optional;

public interface EntityPatcher<R> {

    void apply(R resource);

    default <T> void ignoreUnchangedPropertyValue(final String propertyName, final JsonNullable<T> resourcePropertyValue, final T entityPropertyValue) {
        Optional.ofNullable(resourcePropertyValue).ifPresent(value -> {
            if (!value.get().equals(entityPropertyValue) ) {
                throw new ValidationException(propertyName + " cannot be changed, either leave empty or leave unchanged as " + entityPropertyValue);
            }
        });
    }
}
