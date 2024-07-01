package net.hostsharing.hsadminng.hs.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.hostsharing.hsadminng.hash.LinuxEtcShadowHashGenerator.Algorithm.SHA512;
import static net.hostsharing.hsadminng.hash.LinuxEtcShadowHashGenerator.hash;
import static net.hostsharing.hsadminng.hs.validation.PasswordProperty.passwordProperty;
import static net.hostsharing.hsadminng.mapper.PatchableMapWrapper.entry;
import static org.assertj.core.api.Assertions.assertThat;

class PasswordPropertyUnitTest {

    private final ValidatableProperty<PasswordProperty, String> passwordProp =
            passwordProperty("password").minLength(8).maxLength(40).hashedUsing(SHA512).writeOnly();
    private final List<String> violations = new ArrayList<>();

    @ParameterizedTest
    @ValueSource(strings = {
            "lowerUpperAndDigit1",
            "lowerUpperAndSpecial!",
            "digit1LowerAndSpecial!",
            "digit1special!lower",
            "DIGIT1SPECIAL!UPPER" })
    void shouldValidateValidPassword(final String password) {
        // when
        passwordProp.validate(violations, password, null);

        // then
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "noDigitNoSpecial",
            "!!!!!!12345",
            "nolower-nodigit",
            "nolower1nospecial",
            "NOLOWER-NODIGIT",
            "NOLOWER1NOSPECIAL"
    })
    void shouldRecognizeMissingCharacterGroup(final String givenPassword) {
        // when
        passwordProp.validate(violations, givenPassword, null);

        // then
        assertThat(violations)
                .contains("password' must contain at least one character of at least 3 of the following groups: upper case letters, lower case letters, digits, special characters")
                .doesNotContain(givenPassword);
    }

    @Test
    void shouldRecognizeTooShortPassword() {
        // given
        final String givenPassword = "0123456";

        // when
        passwordProp.validate(violations, givenPassword, null);

        // then
        assertThat(violations)
                .contains("password' length is expected to be at min 8 but length of provided value is 7")
                .doesNotContain(givenPassword);
    }

    @Test
    void shouldRecognizeTooLongPassowrd() {
        // given
        final String givenPassword = "password' length is expected to be at max 40 but is 41";

        // when
        passwordProp.validate(violations, givenPassword, null);

        // then
        assertThat(violations).contains("password' length is expected to be at max 40 but length of provided value is 54")
                .doesNotContain(givenPassword);
    }

    @Test
    void shouldRecognizeColonInPassword() {
        // given
        final String givenPassword = "lowerUpper:1234";

        // when
        passwordProp.validate(violations, givenPassword, null);

        // then
        assertThat(violations)
                .contains("password' must not contain colon (':')")
                .doesNotContain(givenPassword);
    }

    @Test
    void shouldComputeHash() {

        // when
        final var result = passwordProp.compute(new PropertiesProvider() {

            @Override
            public Map<String, Object> directProps() {
                return Map.ofEntries(
                        entry(passwordProp.propertyName, "some password")
                );
            }

            @Override
            public Object getContextValue(final String propName) {
                return null;
            }
        });

        // then
        hash("some password").using(SHA512).withRandomSalt().generate(); // throws exception if wrong
    }
}
