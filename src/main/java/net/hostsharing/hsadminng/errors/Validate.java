package net.hostsharing.hsadminng.errors;

import lombok.AllArgsConstructor;

import jakarta.validation.ValidationException;

@AllArgsConstructor
public class Validate {

    final String variableNames;

    public static Validate validate(final String variableNames) {
        return new Validate(variableNames);
    }

    public final void atMaxOne(final Object var1, final Object var2) {
        if (var1 != null && var2 != null) {
            throw new ValidationException(
                    "At maximum one of (" + variableNames + ") must be non-null, " +
                            "but are (" + var1 + ", " + var2 + ")");
        }
    }

    public final void exactlyOne(final Object var1, final Object var2) {
        if ((var1 != null) == (var2 != null)) {
            throw new ValidationException(
                    "Exactly one of (" + variableNames + ") must be non-null, " +
                            "but are (" + var1 + ", " + var2 + ")");
        }
    }
}
