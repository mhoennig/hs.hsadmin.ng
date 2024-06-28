package net.hostsharing.hsadminng.hs.validation;

import net.hostsharing.hsadminng.hash.HashProcessor.Algorithm;
import lombok.Setter;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hash.HashProcessor.hashAlgorithm;
import static net.hostsharing.hsadminng.mapper.Array.insertAfterEntry;

@Setter
public class PasswordProperty extends StringProperty<PasswordProperty> {

    private static final String[] KEY_ORDER = insertAfterEntry(StringProperty.KEY_ORDER, "computed", "hashedUsing");

    private Algorithm hashedUsing;

    private PasswordProperty(final String propertyName) {
        super(propertyName, KEY_ORDER);
        undisclosed();
    }

    public static PasswordProperty passwordProperty(final String propertyName) {
        return new PasswordProperty(propertyName);
    }

    @Override
    protected void validate(final List<String> result, final String propValue, final PropertiesProvider propProvider) {
        super.validate(result, propValue, propProvider);
        validatePassword(result, propValue);
    }

    public PasswordProperty hashedUsing(final Algorithm algorithm) {
        this.hashedUsing = algorithm;
        // FIXME: computedBy is too late, we need preprocess
        computedBy((entity)
                -> ofNullable(entity.getDirectValue(propertyName, String.class))
                    .map(password -> hashAlgorithm(algorithm).withRandomSalt().generate(password))
                    .orElse(null));
        return self();
    }

    @Override
    protected String simpleTypeName() {
        return "password";
    }

    private void validatePassword(final List<String> result, final String password) {
        boolean hasLowerCase = false;
        boolean hasUpperCase = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;
        boolean containsColon = false;

        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) {
                hasLowerCase = true;
            } else if (Character.isUpperCase(c)) {
                hasUpperCase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isLetterOrDigit(c)) {
                hasSpecialChar = true;
            }

            if (c == ':') {
                containsColon = true;
            }
        }

        final long groupsCovered = Stream.of(hasLowerCase, hasUpperCase, hasDigit, hasSpecialChar).filter(v->v).count();
        if ( groupsCovered < 3) {
            result.add(propertyName + "' must contain at least one character of at least 3 of the following groups: upper case letters, lower case letters, digits, special characters");
        }
        if (containsColon) {
            result.add(propertyName + "' must not contain colon (':')");
        }
    }
}
