package net.hostsharing.hsadminng.repr;


import jakarta.validation.constraints.NotNull;

/**
 * A String value which is used as a symbol and thus does not get quoted
 * and is, by definition, different from any other symbol with the same name.
 */
public class Symbol {
    private final String value;

    public static Symbol symbol(@NotNull final String value) {
        return new Symbol(value);
    }

    private Symbol(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
