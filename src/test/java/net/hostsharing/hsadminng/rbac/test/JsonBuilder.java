package net.hostsharing.hsadminng.rbac.test;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Build JSONObject with little boilerplate code.
 */
public class JsonBuilder {

    private final JSONObject jsonObject;

    /**
     * Create a JsonBuilder from a string.
     *
     * @param jsonString valid JSON
     * @return a new JsonBuilder
     */
    public static JsonBuilder jsonObject(final String jsonString) {
        return new JsonBuilder(jsonString);
    }

    /**
     * Add a string property (key/value pair).
     *
     * @param key   JSON key
     * @param value JSON value
     * @return this JsonBuilder
     */
    public JsonBuilder with(final String key, final String value) {
        try {
            jsonObject.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * Add a numeric property (key/value pair).
     *
     * @param key   JSON key
     * @param value JSON value
     * @return this JsonBuilder
     */
    public JsonBuilder with(final String key, final Number value) {
        try {
            jsonObject.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * Removes a property (key/value pair).
     *
     * @param name JSON key
     * @return this JsonBuilder
     */
    public JsonBuilder without(final String name) {
        jsonObject.remove(name);
        return this;
    }

    @Override
    public String toString() {
        try {
            return jsonObject.toString(4);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonBuilder(final String jsonString) {
        try {
            jsonObject = new JSONObject(jsonString);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
