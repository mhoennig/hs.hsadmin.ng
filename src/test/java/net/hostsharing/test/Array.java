package net.hostsharing.test;

/**
 *  Java has List.of(...), Set.of(...) and Map.of(...) all with varargs parameter,
 *  but no Array.of(...). Here it is.
 */
public class Array {

    @SafeVarargs
    public static <E> E[] of(E... elements) {
        return elements;
    }
}
