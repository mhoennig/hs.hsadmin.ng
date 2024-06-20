package net.hostsharing.hsadminng.errors;

import jakarta.validation.ValidationException;
import java.util.List;

import static java.lang.String.join;

public class MultiValidationException extends ValidationException {

    private MultiValidationException(final List<String> violations) {
        super(
                violations.size() > 1
                    ? "[\n" + join(",\n", violations) + "\n]"
                    : "[" + join(",\n", violations) + "]"
        );
    }

    public static void throwInvalid(final List<String> violations) {
        if (!violations.isEmpty()) {
            throw new MultiValidationException(violations);
        }
    }
}
