package net.hostsharing.hsadminng.lambda;

public class Reducer {
    public static  <T> T toSingleElement(T last, T next) {
        throw new AssertionError("only a single entity expected");
    }

}
