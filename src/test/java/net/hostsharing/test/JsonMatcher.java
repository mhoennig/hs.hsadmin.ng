package net.hostsharing.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class JsonMatcher extends BaseMatcher<CharSequence> {

    private final String expected;
    private JSONCompareMode compareMode;

    public JsonMatcher(final String expected, final JSONCompareMode compareMode) {
        this.expected = expected;
        this.compareMode = compareMode;
    }

    /**
     * Creates a matcher for RestAssured to validate if the actual JSON matches the expected JSON leniently.
     *
     * @see package org.skyscreamer.jsonassert.JSONCompareMode.LENIENT
     *
     * @return the RestAssuredMatcher
     */
    public static Matcher<CharSequence> lenientlyEquals(final String expected) {
        return new JsonMatcher(expected, JSONCompareMode.LENIENT);
    }

    /**
     * Creates a matcher for RestAssured to validate if the actual JSON matches the expected JSON strictly.
     *
     * @see package org.skyscreamer.jsonassert.JSONCompareMode.STRICT
     *
     * @return the RestAssuredMatcher
     */
    public static Matcher<CharSequence> strictlyEquals(final String expected) {
        return new JsonMatcher(expected, JSONCompareMode.STRICT);
    }

    @Override
    public boolean matches(final Object actual) {
        if (actual == null || actual.getClass().isAssignableFrom(CharSequence.class)) {
            return false;
        }
        try {
            final var actualJson = new ObjectMapper().writeValueAsString(actual);
            compareMode = JSONCompareMode.LENIENT;
            JSONAssert.assertEquals(expected, actualJson, compareMode);
            return true;
        } catch (final JSONException | JsonProcessingException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("leniently matches JSON");
    }

}
