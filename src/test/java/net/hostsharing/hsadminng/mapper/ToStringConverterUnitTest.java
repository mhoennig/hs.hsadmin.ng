package net.hostsharing.hsadminng.mapper;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToStringConverterUnitTest {

    @Test
    void convertObjectToString() {
        final var object = new SomeObject("a", 1, true);
        final var result = new ToStringConverter().ignoring("three").from(object);
        assertThat(result).isEqualTo("{ one: a, two: 1 }");
    }

    @Test
    void convertMapToString() {
        final var map = Map.ofEntries(
            Map.entry("one", "a"),
            Map.entry("two", 1),
            Map.entry("three", true)
        );
        final var result = new ToStringConverter().ignoring("three").from(map);
        assertThat(result).isEqualTo("{ one: a, two: 1 }");
    }
}

record SomeObject(String one, int two, boolean three) {}
