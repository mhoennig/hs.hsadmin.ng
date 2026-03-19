package net.hostsharing.hsadminng.mapper;

import org.junit.jupiter.api.Test;

import jakarta.validation.ValidationException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class KeyValueMapUnitTest {

    final ToStringConverter toStringConverter = new ToStringConverter();

    @Test
    void fromMap() {
        final var result = KeyValueMap.from("propName", Map.ofEntries(
            Map.entry("one", 1),
            Map.entry("two", 2)
        ));

        assertThat(toStringConverter.from(result)).isEqualTo("{ one: 1, two: 2 }");
    }

    @Test
    void fromNonMap() {
        final var exception = catchThrowable( () ->
            KeyValueMap.from("propMap", "not a map")
        );

        assertThat(exception).isInstanceOf(ValidationException.class);
        assertThat(exception.getMessage()).isEqualTo("propMap: Map expected, but got: not a map");
    }
}
