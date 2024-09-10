package net.hostsharing.hsadminng.hs.validation;

import lombok.AccessLevel;
import lombok.Setter;
import net.hostsharing.hsadminng.mapper.Array;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.stream;

@Setter
public class StringProperty<P extends StringProperty<P>> extends ValidatableProperty<P, String> {

    protected static final String[] KEY_ORDER = Array.join(
            ValidatableProperty.KEY_ORDER_HEAD,
            Array.of("matchesRegEx", "matchesRegExDescription",
                    "notMatchesRegEx", "notMatchesRegExDescription",
                    "minLength", "maxLength",
                    "provided"),
            ValidatableProperty.KEY_ORDER_TAIL,
            Array.of("undisclosed"));
    private String[] provided;
    private Pattern[] matchesRegEx;
    private String matchesRegExDescription;
    private Pattern[] notMatchesRegEx;
    private String notMatchesRegExDescription;
    @Setter(AccessLevel.PRIVATE)
    private Consumer<String> describedAsConsumer;
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

    public Integer minLength() {
        return this.minLength;
    }

    public P maxLength(final int maxLength) {
        this.maxLength = maxLength;
        return self();
    }

    public Integer maxLength() {
        return this.maxLength;
    }

    public P matchesRegEx(final String... regExPattern) {
        this.matchesRegEx = stream(regExPattern).map(Pattern::compile).toArray(Pattern[]::new);
        this.describedAsConsumer = violationMessage -> matchesRegExDescription = violationMessage;
        return self();
    }

    public P notMatchesRegEx(final String... regExPattern) {
        this.notMatchesRegEx = stream(regExPattern).map(Pattern::compile).toArray(Pattern[]::new);
        this.describedAsConsumer = violationMessage -> notMatchesRegExDescription = violationMessage;
        return self();
    }

    public P describedAs(final String violationMessage) {
        describedAsConsumer.accept(violationMessage);
        describedAsConsumer = null;
        return self();
    }

    /// predefined values, similar to fixed values in a combobox
    public P provided(final String... provided) {
        this.provided = provided;
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
        super.validate(result, propValue, propProvider);
        validateMinLength(result, propValue);
        validateMaxLength(result, propValue);
        validateMatchesRegEx(result, propValue);
        validateNotMatchesRegEx(result, propValue);
    }

    @Override
    protected String display(final String propValue) {
        return undisclosed ? "provided value" : ("'" + propValue + "'");
    }

    @Override
    protected String simpleTypeName() {
        return "string";
    }

    private void validateMinLength(final List<String> result, final String propValue) {
        if (minLength != null && propValue.length()<minLength) {
            result.add(propertyName + "' length is expected to be at min " + minLength + " but length of " + display(propValue) + " is " + propValue.length());
        }
    }

    private void validateMaxLength(final List<String> result, final String propValue) {
        if (maxLength != null && propValue.length()>maxLength) {
            result.add(propertyName + "' length is expected to be at max " + maxLength + " but length of " + display(propValue) + " is " + propValue.length());
        }
    }

    private void validateMatchesRegEx(final List<String> result, final String propValue) {
        if (matchesRegEx != null &&
                stream(matchesRegEx).map(p -> p.matcher(propValue)).noneMatch(Matcher::matches)) {
            if (matchesRegExDescription != null) {
                result.add(propertyName + "' = " + display(propValue) + " " + matchesRegExDescription);
            } else if (matchesRegEx.length>1) {
                result.add(propertyName + "' is expected to match any of " + Arrays.toString(matchesRegEx) +
                        " but " + display(propValue) + " does not match any");
            } else {
                result.add(propertyName + "' is expected to match " + Arrays.toString(matchesRegEx) + " but " + display(
                        propValue) +
                        " does not match");
            }
        }
    }

    private void validateNotMatchesRegEx(final List<String> result, final String propValue) {
        if (notMatchesRegEx != null &&
                stream(notMatchesRegEx).map(p -> p.matcher(propValue)).anyMatch(Matcher::matches)) {
            if (notMatchesRegExDescription != null) {
                result.add(propertyName + "' = " + display(propValue) + " " + notMatchesRegExDescription);
            } else if (notMatchesRegEx.length>1) {
                result.add(propertyName + "' is expected not to match any of " + Arrays.toString(notMatchesRegEx) +
                        " but " + display(propValue) + " does match at least one");
            } else {
                result.add(propertyName + "' is expected not to match " + Arrays.toString(notMatchesRegEx) +
                        " but " + display(propValue) + " does match");
            }
        }
    }
}
