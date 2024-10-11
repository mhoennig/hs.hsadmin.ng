package net.hostsharing.hsadminng.mapper;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class KeyValueMapUnitTest {

    final ToStringConverter toStringConverter = new ToStringConverter();

    @Test
    void fromMap() {
        final var result = KeyValueMap.from(Map.ofEntries(
            Map.entry("one", 1),
            Map.entry("two", 2)
        ));

        assertThat(toStringConverter.from(result)).isEqualTo("{ one: 1, two: 2 }");
    }

    @Test
    void fromNonMap() {
        final var exception = catchThrowable( () ->
            KeyValueMap.from("not a map")
        );

        assertThat(exception).isInstanceOf(ClassCastException.class);
    }
}
