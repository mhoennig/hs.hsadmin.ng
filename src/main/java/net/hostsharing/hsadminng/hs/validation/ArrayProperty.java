package net.hostsharing.hsadminng.hs.validation;

import lombok.Setter;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.stream;
import static net.hostsharing.hsadminng.mapper.Array.insertNewEntriesAfterExistingEntry;

@Setter
public class ArrayProperty<P extends ValidatableProperty<?, E>, E> extends ValidatableProperty<ArrayProperty<P, E>, E[]> {

    private static final String[] KEY_ORDER =
            insertNewEntriesAfterExistingEntry(
                insertNewEntriesAfterExistingEntry(ValidatableProperty.KEY_ORDER, "required", "minLength" ,"maxLength"),
                   "propertyName", "elementsOf");
    private final ValidatableProperty<?, E> elementsOf;
    private Integer minLength;
    private Integer maxLength;

    private ArrayProperty(final ValidatableProperty<?, E> elementsOf) {
        //noinspection unchecked
        super((Class<E[]>) elementsOf.type.arrayType(), elementsOf.propertyName, KEY_ORDER);
        this.elementsOf = elementsOf;
    }

    public static <T> ArrayProperty<?, T[]> arrayOf(final ValidatableProperty<?, T> elementsOf) {
        //noinspection unchecked
        return (ArrayProperty<?, T[]>) new ArrayProperty<>(elementsOf);
    }

    public ValidatableProperty<?, ?> minLength(final int minLength) {
        this.minLength = minLength;
        return self();
    }

    public ValidatableProperty<?, ?> maxLength(final int maxLength) {
        this.maxLength = maxLength;
        return self();
    }

    @Override
    protected void validate(final List<String> result, final E[] propValue, final PropertiesProvider propProvider) {
        if (minLength != null && propValue.length < minLength) {
            result.add(propertyName + "' length is expected to be at min " + minLength + " but length of " + displayArray(propValue) + " is " + propValue.length);
        }
        if (maxLength != null && propValue.length > maxLength) {
            result.add(propertyName + "' length is expected to be at max " + maxLength + " but length of " + displayArray(propValue) + " is " + propValue.length);
        }
        stream(propValue).forEach(e -> elementsOf.validate(result, e, propProvider));
    }

    @Override
    protected String simpleTypeName() {
        return elementsOf.simpleTypeName() + "[]";
    }

    @SafeVarargs
    private String displayArray(final E... propValue) {
        return "[" + Arrays.toString(propValue) + "]";
    }
}
