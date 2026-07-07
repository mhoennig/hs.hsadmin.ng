package net.hostsharing.hsadminng.errors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.validation.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ValidateUnitTest {

    @Nested
    class AtMaxOne {
        @Test
        void shouldFailValidationIfBothParametersAreNotNull() {
            final var throwable = catchThrowable(() ->
                    Validate.validate("var1, var2").atMaxOne("val1", "val2")
            );
            assertThat(throwable).isInstanceOf(ValidationException.class)
                    .hasMessage("At maximum one of (var1, var2) must be non-null, but are (val1, val2)");
        }

        @Test
        void shouldNotFailValidationIfBothParametersAreNull() {
            Validate.validate("var1, var2").atMaxOne(null, null);
        }

        @Test
        void shouldNotFailValidationIfExactlyOneParameterIsNonNull() {
            Validate.validate("var1, var2").atMaxOne("val1", null);
            Validate.validate("var1, var2").atMaxOne(null, "val2");
        }
    }

    @Nested
    class ExactlyOne {
        @Test
        void shouldFailValidationIfBothParametersAreNotNull() {
            final var throwable = catchThrowable(() ->
                    Validate.validate("var1, var2").exactlyOne("val1", "val2")
            );
            assertThat(throwable).isInstanceOf(ValidationException.class)
                    .hasMessage("Exactly one of (var1, var2) must be non-null, but are (val1, val2)");
        }

        @Test
        void shouldFailValidationIfBothParametersAreNull() {
            final var throwable = catchThrowable(() ->
                    Validate.validate("var1, var2").exactlyOne(null, null)
            );
            assertThat(throwable).isInstanceOf(ValidationException.class)
                    .hasMessage("Exactly one of (var1, var2) must be non-null, but are (null, null)");
        }

        @Test
        void shouldNotFailValidationIfExactlyOneParameterIsNonNull() {
            Validate.validate("var1, var2").exactlyOne("val1", null);
            Validate.validate("var1, var2").exactlyOne(null, "val2");
        }
    }

    @Nested
    class AreEqual {
        @Test
        void shouldFailValidationIfParametersAreNotEqual() {
            final var throwable = catchThrowable(() ->
                    Validate.validate("var1, var2").areEqual("val1", "val2")
            );
            assertThat(throwable).isInstanceOf(ValidationException.class)
                    .hasMessage("Both (var1, var2) must be equal, but are (val1, val2)");
        }

        @Test
        void shouldFailValidationIfExactlyOneParameterIsNull() {
            final var throwable = catchThrowable(() ->
                    Validate.validate("var1, var2").areEqual("val1", null)
            );
            assertThat(throwable).isInstanceOf(ValidationException.class)
                    .hasMessage("Both (var1, var2) must be equal, but are (val1, null)");
        }

        @Test
        void shouldNotFailValidationIfParametersAreEqual() {
            Validate.validate("var1, var2").areEqual("val", "val");
        }

        @Test
        void shouldNotFailValidationIfBothParametersAreNull() {
            Validate.validate("var1, var2").areEqual(null, null);
        }
    }
}
