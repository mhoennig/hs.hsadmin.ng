package net.hostsharing.hsadminng.mapper;

import lombok.val;
import org.junit.jupiter.api.Test;

import jakarta.validation.ValidationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PatchableMapWrapperUnitTest {

    /** Jackson deserializes JSON arrays to ArrayList, but array properties validate against String[]. */
    @Test
    void assignConvertsJsonArraysLikePut() {
        // given
        final Map<String, Object> delegate = new HashMap<>();
        val wrapper = PatchableMapWrapper.of(delegate);

        // when
        wrapper.assign(Map.of(
                "target", new ArrayList<>(List.of("xyz00-office", "office@example.com")),
                "caption", "unchanged"));

        // then
        assertThat(delegate.get("target")).isEqualTo(new String[] { "xyz00-office", "office@example.com" });
        assertThat(delegate.get("caption")).isEqualTo("unchanged");
    }

    @Test
    void assignRejectsNullElementInJsonArray() {
        // given
        final Map<String, Object> delegate = new HashMap<>();
        val wrapper = PatchableMapWrapper.of(delegate);
        final Map<String, Object> givenEntries = Map.of(
                "target", new ArrayList<>(Arrays.asList("xyz00-office", null)));

        // when
        val thrown = catchThrowable(() -> wrapper.assign(givenEntries));

        // then
        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessage("'target[1]' must not be null");
        assertThat(delegate).isEmpty();
    }

    /** Values loaded from jsonb arrive as ArrayList as well, thus load-then-PATCH needs the same conversion. */
    @Test
    void getConvertsJsonArraysLikeAssign() {
        // given
        final Map<String, Object> delegate = new HashMap<>();
        delegate.put("target", new ArrayList<>(List.of("xyz00-office", "office@example.com")));
        val wrapper = PatchableMapWrapper.of(delegate);

        // when
        val actualValue = wrapper.get("target");

        // then
        assertThat(actualValue).isEqualTo(new String[] { "xyz00-office", "office@example.com" });
    }

    @Test
    void putAllConvertsJsonArraysLikeAssign() {
        // given
        final Map<String, Object> delegate = new HashMap<>();
        val wrapper = PatchableMapWrapper.of(delegate);

        // when
        wrapper.putAll(Map.of(
                "target", new ArrayList<>(List.of("xyz00-office")),
                "caption", "unchanged"));

        // then
        assertThat(delegate.get("target")).isEqualTo(new String[] { "xyz00-office" });
        assertThat(wrapper.isPatched("target")).isTrue();
    }

    /** A null value means "not given" for PATCH, thus it must mean the same for POST. */
    @Test
    void assignIgnoresNullValuesLikePatch() {
        // given
        final Map<String, Object> delegate = new HashMap<>();
        val wrapper = PatchableMapWrapper.of(delegate);
        final Map<String, Object> givenEntries = new HashMap<>();
        givenEntries.put("target", null);
        givenEntries.put("caption", "unchanged");

        // when
        wrapper.assign(givenEntries);

        // then
        assertThat(delegate).containsExactly(Map.entry("caption", "unchanged"));
    }

    @Test
    void putRejectsNullElementInJsonArray() {
        // given
        final Map<String, Object> delegate = new HashMap<>();
        val wrapper = PatchableMapWrapper.of(delegate);

        // when
        val thrown = catchThrowable(() -> wrapper.put("target", new ArrayList<>(Arrays.asList(null, "xyz00-office"))));

        // then
        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessage("'target[0]' must not be null");
        assertThat(delegate).isEmpty();
    }
}
