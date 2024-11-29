package net.hostsharing.hsadminng.hs.scenarios;


public final class JsonOptional<V> {

    private final boolean jsonValueGiven;
    private final V jsonValue;

    private JsonOptional() {
        this.jsonValueGiven = false;
        this.jsonValue = null;
    }

    private JsonOptional(final V jsonValue) {
        this.jsonValueGiven = true;
        this.jsonValue = jsonValue;
    }

    public static <T> JsonOptional<T> ofValue(final T value) {
        return new JsonOptional<>(value);
    }

    public static <T> JsonOptional<T> notGiven() {
        return new JsonOptional<>();
    }

    public V given() {
        if (!jsonValueGiven) {
            throw new IllegalStateException("JSON value was not given");
        }
        return jsonValue;
    }

    public String givenAsString() {
        if (jsonValue instanceof Double doubleValue) {
            if (doubleValue % 1 == 0) {
                return String.valueOf(doubleValue.intValue()); // avoid trailing ".0"
            } else {
                return doubleValue.toString();
            }
        }
        return jsonValue == null ? null : jsonValue.toString();
    }
}
