package net.hostsharing.hsadminng.hash;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Base64UtilsUnitTest {

    @Test
    void testNullInput() {
        val given = (String) null;
        val actual = Base64Utils.isBase64(given);
        assertThat(actual).as("Null input should not be valid Base64").isFalse();
    }

    @Test
    void testEmptyInput() {
        val given = "";
        val actual = Base64Utils.isBase64(given);
        assertThat(actual).as("Empty string should not be valid Base64").isFalse();
    }

    @Test
    void testValidBase64WithoutPadding() {
        val given = "U29tZU5vbmRLZXk"; // 'SomeNonKey' in Base64
        val actual = Base64Utils.isBase64(given);
        assertThat(actual).as("Valid Base64 string without padding should be identified as valid").isTrue();
    }

    @Test
    void testValidBase64WithPadding() {
        val given = "U29tZSBLZXk="; // 'Some Key' in Base64
        val actual = Base64Utils.isBase64(given);
        assertThat(actual).as("Valid Base64 string with padding should be identified as valid").isTrue();
    }

    @Test
    void testValidBase64WithDoublePadding() {
        val given = "U29tZQ=="; // 'Some' in Base64
        val actual = Base64Utils.isBase64(given);
        assertThat(actual).as("Valid Base64 string with double padding should be identified as valid").isTrue();
    }

    @Test
    void testInvalidBase64LengthNotDivisibleByFour() {
        val given = "U29tZQ="; // Invalid length for Base64 with padding
        val actual = Base64Utils.isBase64(given);
        assertThat(actual).as("Input with invalid length not divisible by 4 should not be valid Base64").isFalse();
    }

    @Test
    void testInvalidBase64WithSpecialCharacters() {
        val given = "U29tZQ$$"; // Contains invalid characters
        val actual = Base64Utils.isBase64(given);
        assertThat(actual).as("Input containing invalid characters should not be valid Base64").isFalse();
    }

    @Test
    void testInvalidBase64WithWhitespace() {
        val given = "U29tZ SBs"; // Contains whitespace
        val actual = Base64Utils.isBase64(given);
        assertThat(actual).as("Input containing whitespace should not be valid Base64").isFalse();
    }

    @Test
    void testInvalidBase64WithExcessivePadding() {
        val given = "U29tZQ==="; // Too many padding characters
        val actual = Base64Utils.isBase64(given);
        assertThat(actual).as("Input with excessive padding should not be valid Base64").isFalse();
    }

    @Test
    void testEdgeCaseBase64SingleCharacter() {
        val given = "U"; // Single valid Base64 character
        val actual = Base64Utils.isBase64(given);
        assertThat(actual).as("Single-character input should not be valid Base64").isFalse();
    }

    @Test
    void testValidBase64EdgeCaseMinimalLengthWithPadding() {
        val given = "U2="; // Minimal valid Base64 with padding – maps to one byte
        val actual = Base64Utils.isBase64(given);
        assertThat(actual).as("Base64 input length not divisible by 4 should fail even if contains padding.").isFalse();
    }
}
