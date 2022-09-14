package net.hostsharing.test;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.UUID;
import java.util.function.Predicate;

public class IsValidUuidMatcher extends BaseMatcher<CharSequence> {

    /**
     * Checks if the given String represents a valid UUID.
     * @param actual the given String
     * @return true if valid UUID, false otherwise
     */
    public static boolean isUuidValid(final String actual) {
        try {
            UUID.fromString(actual);
        } catch (final IllegalArgumentException exc) {
            return false;
        }
        return true;
    }

    /**
     * Creates a matcher for RestAssured to validate if String is a valid UUID.
     *
     * @return the RestAssuredMatcher
     */
    public static Matcher<CharSequence> isUuidValid() {
        return new IsValidUuidMatcher();
    }

    /**
     * Creates matcher for AssertJ to validate if String is a valid UUID.
     *
     * @return the Predicate to be used as a matcher in AssertJ
     */
    public static Predicate<String> isValidUuid() {
        return IsValidUuidMatcher::isUuidValid;
    }

    @Override
    public boolean matches(final Object actual) {
        if (actual == null || actual.getClass().isAssignableFrom(CharSequence.class)) {
            return false;
        }
        return isUuidValid(actual.toString());
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("valid UUID");
    }

}
