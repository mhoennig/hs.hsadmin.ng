package net.hostsharing.hsadminng.mapper;

import java.util.*;

import static java.util.stream.Collectors.joining;

public class ToStringConverter {

    final public Set<String> ignoredFields = new HashSet<>();

    public ToStringConverter ignoring(final String fieldName) {
        ignoredFields.add(fieldName);
        return this;
    }

    public String from(final Object obj) {
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

    public String from(final Map<?, ?> map) {
        return "{ "
            + map.keySet().stream()
                .filter(key -> !ignoredFields.contains(key.toString()))
                .sorted()
                .map(k -> Map.entry(k, map.get(k)))
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(joining(", "))
            + " }";
    }
}
