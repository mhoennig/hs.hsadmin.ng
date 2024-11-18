package net.hostsharing.hsadminng.repr;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TaggedNumber {

    public static Integer cropTag(final String tag, final String taggedNumber) {
        return taggedNumber.startsWith(tag) ? Integer.valueOf(taggedNumber.substring(tag.length())) : invalidTag(tag, taggedNumber);
    }

    private static Integer invalidTag(final String tag, final String taggedNumber) {
        throw new IllegalArgumentException("Expected " + tag + "... but got: " + taggedNumber);
    }
}
