package net.hostsharing.hsadminng.hs.validation;

import lombok.Setter;

import java.util.List;
import java.util.stream.Stream;

@Setter
public class PasswordProperty extends StringProperty {

    private PasswordProperty(final String propertyName) {
        super(propertyName);
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

    // TODO.impl: only a SHA512 hash should be stored in the database, not the password itself

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
