package net.hostsharing.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Java has List.of(...), Set.of(...) and Map.of(...) all with varargs parameter,
 * but no Array.of(...). Here it is.
 */
public class Array {

    @SafeVarargs
    public static <E> E[] of(E... elements) {
        return elements;
    }

    public static String[] from(final List<String> initialList, final String... additionalStrings) {
        final var resultList = new ArrayList<>(initialList);
        resultList.addAll(Arrays.stream(additionalStrings).toList());
        return resultList.toArray(String[]::new);
    }

    public static String[] fromSkippingNull(final List<String> initialList, final String... additionalStrings) {
        final var resultList = new ArrayList<>(initialList);
        resultList.addAll(Arrays.stream(additionalStrings)
                .filter(Objects::nonNull)
                .map(s -> s.replaceAll("  *", " "))
                .toList());
        return resultList.toArray(String[]::new);
    }

    public static String[] from(final String[] initialStrings, final String... additionalStrings) {
        final var resultList = Arrays.asList(initialStrings);
        resultList.addAll(Arrays.stream(additionalStrings).toList());
        return resultList.toArray(String[]::new);
    }

}
