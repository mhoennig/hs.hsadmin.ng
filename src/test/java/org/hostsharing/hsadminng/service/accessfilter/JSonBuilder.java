package org.hostsharing.hsadminng.service.accessfilter;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class JSonBuilder {

    @SafeVarargs
    public static String asJSon(final ImmutablePair<String, Object>... properties) {
        final StringBuilder json = new StringBuilder();
        for (ImmutablePair<String, Object> prop : properties) {
            json.append(inQuotes(prop.left));
            json.append(": ");
            if (prop.right instanceof Number) {
                json.append(prop.right);
            } else {
                json.append(inQuotes(prop.right));
            }
            json.append(",\n");
        }
        return "{\n" + json.substring(0, json.length() - 2) + "\n}";
    }

    private static String inQuotes(Object value) {
        return value != null ? "\"" + value.toString() + "\"" : "null";
    }

}
