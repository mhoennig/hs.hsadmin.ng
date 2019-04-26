package org.hostsharing.hsadminng.service.accessfilter;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;

public class JSonBuilder {

    @SafeVarargs
    public static String asJSon(final ImmutablePair<String, Object>... properties) {
        final StringBuilder json = new StringBuilder();
        for (ImmutablePair<String, Object> prop : properties) {
            json.append(inQuotes(prop.left));
            json.append(": ");
            if (prop.right instanceof Number) {
                json.append(prop.right);
            } else if (prop.right instanceof List)  {
                json.append("[");
                for ( int n = 0; n < ((List<Integer>)prop.right).size(); ++n ) {
                    if ( n > 0 ) {
                        json.append(",");
                    }
                    json.append(((List<Integer>)prop.right).get(n));
                }
                json.append("]");
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
