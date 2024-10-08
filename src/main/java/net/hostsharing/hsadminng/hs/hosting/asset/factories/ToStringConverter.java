package net.hostsharing.hsadminng.hs.hosting.asset.factories;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.joining;

public class ToStringConverter {

    final public Set<String> ignoredFields = new HashSet<>();

    public ToStringConverter ignoring(final String fieldName) {
        ignoredFields.add(fieldName);
        return this;
    }

    public String from(Object obj) {
        StringBuilder result = new StringBuilder();
        return "{ " +
            Arrays.stream(obj.getClass().getDeclaredFields())
                    .filter(f -> !ignoredFields.contains(f.getName()))
                    .map(field -> {
                        try {
                            field.setAccessible(true);
                            return field.getName() + ": " + field.get(obj);
                        } catch (IllegalAccessException e) {
                            // ignore inaccessible fields
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(joining(", "))
        + " }";
    }
}
