package net.hostsharing.hsadminng.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import net.hostsharing.hsadminng.errors.MultiValidationException;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class StrictBodyConverterUnitTest {

    public static class SomeResource {

        @NotNull
        @Pattern(regexp = "^[a-z]+$")
        public String name;
    }

    final StrictBodyConverter converter = new StrictBodyConverter(
            new ObjectMapper(),
            Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void convertsAndValidatesValidBody() {
        // when
        val resource = converter.convertAndValidate(Map.of("name", "valid"), SomeResource.class);

        // then
        assertThat(resource.name).isEqualTo("valid");
    }

    @Test
    void rejectsUnknownPropertyWithBadRequest() {
        // when
        val exception = catchThrowableOfType(
                ResponseStatusException.class,
                () -> converter.convertAndValidate(Map.of("name", "valid", "unknown", true), SomeResource.class));

        // then, unknown properties are rejected like `additionalProperties: false`
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("unknown");
    }

    @Test
    void rejectsMissingRequiredPropertyWithDefaultViolationMessage() {
        assertThatThrownBy(() -> converter.convertAndValidate(Map.of(), SomeResource.class))
                .isInstanceOf(MultiValidationException.class)
                .hasMessageContaining("name must not be null");
    }

    @Test
    void rejectsPatternViolationWithDefaultViolationMessage() {
        assertThatThrownBy(() -> converter.convertAndValidate(Map.of("name", "INVALID"), SomeResource.class))
                .isInstanceOf(MultiValidationException.class)
                .hasMessageContaining("name must match");
    }

    @Test
    void rendersViolationsWithGivenCustomMessageFunction() {
        assertThatThrownBy(() -> converter.convertAndValidate(
                Map.of("name", "INVALID"),
                SomeResource.class,
                violation -> "custom message for " + violation.getInvalidValue()))
                .isInstanceOf(MultiValidationException.class)
                .hasMessageContaining("custom message for INVALID");
    }
}
