package net.hostsharing.hsadminng.hs.validation;

import lombok.Setter;
import net.hostsharing.hsadminng.mapper.Array;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.stream;

@Setter
public class StringProperty<P extends StringProperty<P>> extends ValidatableProperty<P, String> {

    protected static final String[] KEY_ORDER = Array.join(
            ValidatableProperty.KEY_ORDER_HEAD,
            Array.of("matchesRegEx", "minLength", "maxLength"),
            ValidatableProperty.KEY_ORDER_TAIL,
            Array.of("undisclosed"));
    private Pattern[] matchesRegEx;
    private Integer minLength;
    private Integer maxLength;
    private boolean undisclosed;

    protected StringProperty(final String propertyName) {
        super(String.class, propertyName, KEY_ORDER);
    }

    protected StringProperty(final String propertyName, final String[] keyOrder) {
        super(String.class, propertyName, keyOrder);
    }

    public static StringProperty<?> stringProperty(final String propertyName) {
        return new StringProperty<>(propertyName);
    }

    public P minLength(final int minLength) {
        this.minLength = minLength;
        return self();
    }

    public P maxLength(final int maxLength) {
        this.maxLength = maxLength;
        return self();
    }

    public P matchesRegEx(final String... regExPattern) {
        this.matchesRegEx = stream(regExPattern).map(Pattern::compile).toArray(Pattern[]::new);
        return self();
    }

    /**
     * The property value is not disclosed in error messages.
     *
     * @return this;
     */
    public P undisclosed() {
        this.undisclosed = true;
        return self();
    }

    @Override
    protected void validate(final List<String> result, final String propValue, final PropertiesProvider propProvider) {
        if (minLength != null && propValue.length()<minLength) {
            result.add(propertyName + "' length is expected to be at min " + minLength + " but length of " + display(propValue) + " is " + propValue.length());
        }
        if (maxLength != null && propValue.length()>maxLength) {
            result.add(propertyName + "' length is expected to be at max " + maxLength + " but length of " + display(propValue) + " is " + propValue.length());
        }
        if (matchesRegEx != null &&
                stream(matchesRegEx).map(p -> p.matcher(propValue)).noneMatch(Matcher::matches)) {
            result.add(propertyName + "' is expected to match any of " + Arrays.toString(matchesRegEx) + " but " + display(propValue) + " does not match any");
        }
        if (isReadOnly() && propValue != null) {
            result.add(propertyName + "' is readonly but given as " + display(propValue));
        }
    }

    private String display(final String propValue) {
        return undisclosed ? "provided value" : ("'" + propValue + "'");
    }

    @Override
    protected String simpleTypeName() {
        return "string";
    }
}
