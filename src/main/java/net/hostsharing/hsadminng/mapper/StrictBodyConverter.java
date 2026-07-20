package net.hostsharing.hsadminng.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import net.hostsharing.hsadminng.errors.MultiValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Applies the OpenAPI schema constraints to request bodies which the generator emits as a bare {@code Object}
 * (e.g. {@code anyOf} bodies), where {@code @Valid} thus cannot run.
 *
 * <p>The untyped body is strictly converted to the given generated resource class — rejecting unknown
 * properties ({@code additionalProperties: false}) and invalid enum values — and then bean-validated
 * programmatically, so the annotations generated from the OpenAPI schema ({@code @NotNull}, {@code @Pattern})
 * fire just like {@code @Valid} would. Violations are rejected with a 400 response.</p>
 *
 * <p>For {@code anyOf} bodies, the caller dispatches on its discriminator property to the matching
 * generated member resource class; a custom violation-message function allows domain-specific
 * (e.g. translated) error messages.</p>
 */
@Component
public class StrictBodyConverter {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public StrictBodyConverter(@Autowired final ObjectMapper objectMapper, @Autowired final Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public <R> R convertAndValidate(final Object body, final Class<R> resourceClass) {
        return convertAndValidate(body, resourceClass, StrictBodyConverter::defaultViolationMessage);
    }

    public <R> R convertAndValidate(
            final Object body,
            final Class<R> resourceClass,
            final Function<ConstraintViolation<R>, String> violationMessage) {
        final var resource = strictConvert(body, resourceClass);
        MultiValidationException.throwIfNotEmpty(
                validator.validate(resource).stream()
                        .map(violationMessage)
                        .toList());
        return resource;
    }

    public static String defaultViolationMessage(final ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + " " + violation.getMessage();
    }

    private <R> R strictConvert(final Object body, final Class<R> resourceClass) {
        try {
            final JsonNode bodyTree = objectMapper.valueToTree(body);
            // rejecting unknown properties enforces the schema's `additionalProperties: false`
            return objectMapper.readerFor(resourceClass)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(bodyTree);
        } catch (final ValueInstantiationException exc) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, invalidValueMessage(exc), exc);
        } catch (final IOException | IllegalArgumentException exc) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exc.getMessage(), exc);
        }
    }

    // e.g. `scopes[1] 'foo:bar' is not valid, valid values are: [rbac.subjects:sync, *:read]`
    // instead of Jackson's "Cannot construct instance of ..." with the full reference chain
    private String invalidValueMessage(final ValueInstantiationException exc) {
        final var path = exc.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining());
        final var targetClass = exc.getType().getRawClass();
        if (targetClass.isEnum()) {
            // the generated enums' @JsonCreator throws IllegalArgumentException(value), thus
            // the cause's message is the invalid value itself
            final var invalidValue = exc.getCause() != null ? exc.getCause().getMessage() : exc.getOriginalMessage();
            final var validValues = Arrays.stream(targetClass.getEnumConstants())
                    .map(constant -> objectMapper.convertValue(constant, String.class))
                    .toList();
            return path + " '" + invalidValue + "' is not valid, valid values are: " + validValues;
        }
        return path + " " + exc.getOriginalMessage();
    }
}
