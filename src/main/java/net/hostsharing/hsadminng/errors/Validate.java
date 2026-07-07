package net.hostsharing.hsadminng.errors;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

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

    public final void areEqual(final Object var1, final Object var2) {
        if (ObjectUtils.notEqual(var1, var2)) {
            throw new ValidationException(
                    "Both (" + variableNames + ") must be equal, " +
                            "but are (" + var1 + ", " + var2 + ")");
        }
    }
}
