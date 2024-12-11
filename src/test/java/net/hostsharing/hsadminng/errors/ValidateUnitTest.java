package net.hostsharing.hsadminng.errors;

import org.junit.jupiter.api.Test;

import jakarta.validation.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ValidateUnitTest {

    @Test
    void shouldFailValidationIfBothParametersAreNotNull() {
        final var throwable = catchThrowable(() ->
            Validate.validate("var1, var2").atMaxOneNonNull("val1", "val2")
        );
        assertThat(throwable).isInstanceOf(ValidationException.class)
                .hasMessage("Exactly one of (var1, var2) must be non-null, but are (val1, val2)");
    }

    @Test
    void shouldNotFailValidationIfBothParametersAreull() {
        Validate.validate("var1, var2").atMaxOneNonNull(null, null);
    }

    @Test
    void shouldNotFailValidationIfExactlyOneParameterIsNonNull() {
        Validate.validate("var1, var2").atMaxOneNonNull("val1", null);
        Validate.validate("var1, var2").atMaxOneNonNull(null, "val2");
    }
}
