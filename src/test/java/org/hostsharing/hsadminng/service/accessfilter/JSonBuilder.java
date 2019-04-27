package org.hostsharing.hsadminng.service.accessfilter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;

public class JSonBuilder {

    private StringBuilder json = new StringBuilder();

    @SafeVarargs
    public static String asJSon(final ImmutablePair<String, Object>... properties) {
        final StringBuilder json = new StringBuilder();
        for (ImmutablePair<String, Object> prop : properties) {
            json.append(inQuotes(prop.left));
            json.append(": ");
            if (prop.right instanceof Number) {
                json.append(prop.right);
            } else if (prop.right instanceof List) {
                json.append(toJSonArray(prop.right));
            } else {
                json.append(inQuotes(prop.right));
            }
            json.append(",\n");
        }
        return "{\n" + json.substring(0, json.length() - 2) + "\n}";
    }

    public JSonBuilder withFieldValue(String name, String value) {
        json.append(inQuotes(name) + ":" + (value != null ? inQuotes(value) : "null") + ",");
        return this;
    }

    public JSonBuilder withFieldValue(String name, Number value) {
        json.append(inQuotes(name) + ":" + (value != null ? value : "null") + ",");
        return this;
    }

    public JSonBuilder toJSonNullFieldDefinition(String name) {
        json.append(inQuotes(name) + ":null,");
        return this;
    }

    public JSonBuilder withFieldValueIfPresent(String name, String value) {
        json.append(value != null ? inQuotes(name) + ":" + inQuotes(value) + "," : "");
        return this;
    }

    public JSonBuilder withFieldValueIfPresent(String name, Number value) {
        json.append(value != null ? inQuotes(name) + ":" + value + "," : "");
        return this;
    }

    @Override
    public String toString() {
        return "{" + StringUtils.removeEnd(json.toString(), ",") + "}";
    }

    @SuppressWarnings("unchecked")
    // currently just for the case of date values represented as arrays of integer
    private static String toJSonArray(final Object value) {
        final StringBuilder jsonArray = new StringBuilder("[");
        for (int n = 0; n < ((List<Integer>) value).size(); ++n) {
            if (n > 0) {
                jsonArray.append(",");
            }
            jsonArray.append(((List<Integer>) value).get(n));
        }
        return jsonArray.toString() + "]";
    }


    private static String inQuotes(Object value) {
        return value != null ? "\"" + value.toString() + "\"" : "null";
    }

}
