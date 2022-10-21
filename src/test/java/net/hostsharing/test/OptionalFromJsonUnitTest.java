package net.hostsharing.test;

import net.hostsharing.hsadminng.mapper.OptionalFromJson;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

import static org.assertj.core.api.Assertions.assertThat;

class OptionalFromJsonUnitTest {

    private String value = "unchanged initial value";

    @Test
    void shouldHandleActualValue() {
        // given
        final JsonNullable<String> given = JsonNullable.of("actual new value");

        // when
        OptionalFromJson.of(given).ifPresent(valueFromJson -> value = valueFromJson);

        // then
        assertThat(value).isEqualTo("actual new value");
    }

    @Test
    void shouldHandleNullValue() {
        // given
        final JsonNullable<String> given = JsonNullable.of(null);

        // when
        OptionalFromJson.of(given).ifPresent(valueFromJson -> value = valueFromJson);

        // then
        assertThat(value).isNull();
    }

    @Test
    void shouldHandleUndefinedValue() {
        // given - what should have happen in the JSON mapper
        final JsonNullable<String> given = JsonNullable.undefined();

        // when
        OptionalFromJson.of(given).ifPresent(valueFromJson -> value = valueFromJson);

        // then
        assertThat(value).isEqualTo("unchanged initial value");
    }

    @Test
    void shouldHandleInvalidNullJsonNullable() {
        // given - what seems to happen in the JSON mapper
        final JsonNullable<String> given = null;

        // when
        OptionalFromJson.of(given).ifPresent(valueFromJson -> value = valueFromJson);

        // then
        assertThat(value).isEqualTo("unchanged initial value");
    }
}
