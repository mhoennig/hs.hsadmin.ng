package net.hostsharing.hsadminng.lambda;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Reducer {
    public static  <T> T toSingleElement(T ignoredLast, T ignoredNext) {
        throw new AssertionError("only a single entity expected");
    }

}
