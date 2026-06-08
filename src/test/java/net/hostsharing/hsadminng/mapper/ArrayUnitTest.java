package net.hostsharing.hsadminng.mapper;

import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArrayUnitTest {

    @Test
    void ofReturnsGivenElementsAsArray() {
        // when
        val result = Array.of("a", "b");

        // then
        assertThat(result).containsExactly("a", "b");
    }

    @Test
    void fromListAppendsAdditionalStrings() {
        // when
        val result = Array.from(List.of("a", "b"), "c", "d");

        // then
        assertThat(result).containsExactly("a", "b", "c", "d");
    }

    @Test
    void fromFormattedAppendsNormalizedNonNullStrings() {
        // when
        val result = Array.fromFormatted(List.of("a"), "b   c", null, "d  e");

        // then
        assertThat(result).containsExactly("a", "b c", "d e");
    }

    @Test
    void fromArrayCurrentlyRejectsAdditionalStringsBecauseListIsFixedSize() {
        // then
        assertThatThrownBy(() -> Array.from(new String[] { "a" }, "b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void joinConcatenatesArrayParts() {
        // when
        val result = Array.join(new String[] { "a", "b" }, new String[] { "c" });

        // then
        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    void emptyArrayReturnsGenericEmptyArray() {
        // when
        val result = Array.emptyArray();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void emptyArrayWithElementClassReturnsTypedEmptyArray() {
        // when
        val result = Array.emptyArray(String.class);

        // then
        assertThat(result).isEmpty();
        assertThat(result).isInstanceOf(String[].class);
    }

    @Test
    void insertNewEntriesAfterExistingEntryInsertsAfterMatch() {
        // when
        val result = Array.insertNewEntriesAfterExistingEntry(new String[] { "a", "d" }, "a", "b", "c");

        // then
        assertThat(result).containsExactly("a", "b", "c", "d");
    }

    @Test
    void insertNewEntriesAfterExistingEntryRejectsMissingEntry() {
        // then
        assertThatThrownBy(() -> Array.insertNewEntriesAfterExistingEntry(new String[] { "a" }, "x", "b"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("entry x not found in [a]");
    }
}
