package net.hostsharing.hsadminng.mapper;

import org.openapitools.jackson.nullable.JsonNullable;

import java.util.function.Consumer;

public class OptionalFromJson<T> {

    private final JsonNullable<T> optionalNullableValueFromJson;

    public OptionalFromJson(final JsonNullable<T> optionalNullableValueFromJson) {
        this.optionalNullableValueFromJson = optionalNullableValueFromJson;
    }

    public static <T> OptionalFromJson<T> of(final JsonNullable<T> optionalNullableValueFromJson) {
        return new OptionalFromJson<>(optionalNullableValueFromJson);
    }

    public void ifPresent(final Consumer<T> setter) {
        // It looks like a bug to me, that the JsonNullable itself is null if the element was not in the JSON;
        //  and if it is not null, isPresent() always returns true and thus ifPresent() always fires.
        // Instead there should always be a JsonNullable instance
        //  and ifPresent() should only fire if the element was actually in th JSON.
        if (optionalNullableValueFromJson != null) {
            // this will work with the bug as well as without
            optionalNullableValueFromJson.ifPresent(setter);
        }
    }
}
