package net.hostsharing.test;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.UUID;

public class IsValidUuidMatcher extends BaseMatcher<CharSequence> {

    public static Matcher<CharSequence> isValidUuid() {
        return new IsValidUuidMatcher();
    }

    public static boolean isValidUuid(final String actual) {
        try {
            UUID.fromString(actual);
        } catch (final IllegalArgumentException exc) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(final Object actual) {
        if (actual == null || actual.getClass().isAssignableFrom(CharSequence.class)) {
            return false;
        }
        return isValidUuid(actual.toString());
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("valid UUID");
    }

}
