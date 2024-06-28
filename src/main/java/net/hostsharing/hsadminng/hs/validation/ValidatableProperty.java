package net.hostsharing.hsadminng.hs.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.experimental.Accessors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.mapper.Array;
import org.apache.commons.lang3.function.TriFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

@Getter
@RequiredArgsConstructor
public abstract class ValidatableProperty<P extends ValidatableProperty<?, ?>, T> {

    protected static final String[] KEY_ORDER_HEAD = Array.of("propertyName");
    protected static final String[] KEY_ORDER_TAIL = Array.of("required", "defaultValue", "readOnly", "writeOnly", "computed", "isTotalsValidator", "thresholdPercentage");

    final Class<T> type;
    final String propertyName;

    @JsonIgnore
    private final String[] keyOrder;

    private Boolean required;
    private T defaultValue;

    @JsonIgnore
    private Function<PropertiesProvider, T> computedBy;

    @Accessors(makeFinal = true, chain = true, fluent = false)
    private boolean computed; // used in descriptor, because computedBy cannot be rendered to a text string

    @Accessors(makeFinal = true, chain = true, fluent = false)
    private boolean readOnly;

    @Accessors(makeFinal = true, chain = true, fluent = false)
    private boolean writeOnly;

    private Function<ValidatableProperty<?, ?>[], T[]> deferredInit;
    private boolean isTotalsValidator = false;

    @JsonIgnore
    private List<Function<HsBookingItemEntity, List<String>>> asTotalLimitValidators; // TODO.impl: move to BookingItemIntegerProperty

    private Integer thresholdPercentage; // TODO.impl: move to IntegerProperty

    public final P self() {
        //noinspection unchecked
        return (P) this;
    }

    public String unit() {
        return null;
    }

protected void setDeferredInit(final Function<ValidatableProperty<?, ?>[], T[]> function) {
        this.deferredInit = function;
    }

    public boolean hasDeferredInit() {
        return deferredInit != null;
    }

    public T[] doDeferredInit(final ValidatableProperty<?, ?>[] allProperties) {
        return deferredInit.apply(allProperties);
    }

    public P writeOnly() {
        this.writeOnly = true;
        optional();
        return self();
    }

    public P readOnly() {
        this.readOnly = true;
        optional();
        return self();
    }

    public P required() {
        required = TRUE;
        return self();
    }

    public ValidatableProperty<P, T> optional() {
        required = FALSE;
        return this;
    }

    public P withDefault(final T value) {
        defaultValue = value;
        required = FALSE;
        return self();
    }

    public void deferredInit(final ValidatableProperty<?, ?>[] allProperties) {
    }

    public P asTotalLimit() {
        isTotalsValidator = true;
        return self();
    }

    public P asTotalLimitFor(final String propertyName, final String propertyValue) {
        if (asTotalLimitValidators == null) {
            asTotalLimitValidators = new ArrayList<>();
        }
        final TriFunction<HsBookingItemEntity, IntegerProperty, Integer, List<String>> validator =
                (final HsBookingItemEntity entity, final IntegerProperty prop, final Integer factor) -> {

            final var total = entity.getSubBookingItems().stream()
                    .map(server -> server.getResources().get(propertyName))
                    .filter(propertyValue::equals)
                    .count();

            final long limitingValue = ofNullable(prop.getValue(entity.getResources())).orElse(0);
            if (total > factor*limitingValue) {
                return List.of(
                    prop.propertyName() + " maximum total is " + (factor*limitingValue) + ", but actual total for " + propertyName + "=" + propertyValue + " is " + total
                );
            }
            return emptyList();
        };
        asTotalLimitValidators.add((final HsBookingItemEntity entity) -> validator.apply(entity, (IntegerProperty)this,  1));
        return self();
    }

    public String propertyName() {
        return propertyName;
    }

    public boolean isTotalsValidator() {
        return isTotalsValidator || asTotalLimitValidators != null;
    }

    public Integer thresholdPercentage() {
        return thresholdPercentage;
    }

    public ValidatableProperty<P, T> eachComprising(final int factor, final TriFunction<HsBookingItemEntity, IntegerProperty, Integer, List<String>> validator) {
        if (asTotalLimitValidators == null) {
            asTotalLimitValidators = new ArrayList<>();
        }
        asTotalLimitValidators.add((final HsBookingItemEntity entity) -> validator.apply(entity, (IntegerProperty)this,  factor));
        return this;
    }

    public P withThreshold(final Integer percentage) {
        this.thresholdPercentage = percentage;
        return self();
    }

    public final List<String> validate(final PropertiesProvider propsProvider) {
        final var result = new ArrayList<String>();
        final var props = propsProvider.directProps();
        final var propValue = props.get(propertyName);
        if (propValue == null) {
            if (required) {
                result.add(propertyName + "' is required but missing");
            }
        }
        if (propValue != null){
            if ( type.isInstance(propValue)) {
                //noinspection unchecked
                validate(result, (T) propValue, propsProvider);
            } else {
                result.add(propertyName + "' is expected to be of type " + type + ", " +
                        "but is of type '" + propValue.getClass().getSimpleName() + "'");
            }
        }
        return result;
    }

    protected abstract void validate(final List<String> result, final T propValue, final PropertiesProvider propProvider);

    public void verifyConsistency(final Map.Entry<? extends Enum<?>, ?> typeDef) {
        if (required == null ) {
            throw new IllegalStateException(typeDef.getKey() + "[" + propertyName + "] not fully initialized, please call either .required() or .optional()" );
        }
    }

    @SuppressWarnings("unchecked")
    public T getValue(final Map<String, Object> propValues) {
        return (T) Optional.ofNullable(propValues.get(propertyName)).orElse(defaultValue);
    }

    protected abstract String simpleTypeName();

    public Map<String, Object> toOrderedMap() {
            Map<String, Object> sortedMap = new LinkedHashMap<>();
            sortedMap.put("type", simpleTypeName());

            // Add entries according to the given order
            for (String key : keyOrder) {
                final Optional<Object> propValue = getPropertyValue(key);
                propValue.filter(ValidatableProperty::isToBeRendered).ifPresent(o -> sortedMap.put(key, o));
            }

            return sortedMap;
    }

    private static boolean isToBeRendered(final Object v) {
        return !(v instanceof Boolean b) || b;
    }

    @SneakyThrows
    private Optional<Object> getPropertyValue(final String key) {
        return getPropertyValue(getClass(), key);
    }

    @SneakyThrows
    private Optional<Object> getPropertyValue(final Class<?> clazz, final String key) {
        try {
            final var field = clazz.getDeclaredField(key);
            field.setAccessible(true);
            return Optional.ofNullable(arrayToList(field.get(this)));
        } catch (final NoSuchFieldException exc) {
            if (clazz.getSuperclass() != null) {
                return getPropertyValue(clazz.getSuperclass(), key);
            }
            throw exc;
        }
    }

    private Object arrayToList(final Object value) {
        if ( value instanceof String[]) {
            return List.of((String[])value);
        }
        return value;
    }

    public List<String> validateTotals(final HsBookingItemEntity bookingItem) {
        if (asTotalLimitValidators==null) {
            return emptyList();
        }
        return asTotalLimitValidators.stream()
                .map(v -> v.apply(bookingItem))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();
    }

    public P computedBy(final Function<PropertiesProvider, T> compute) {
        this.computedBy = compute;
        this.computed = true;
        return self();
    }

    public <E extends PropertiesProvider> T compute(final E entity) {
        return computedBy.apply(entity);
    }
}
