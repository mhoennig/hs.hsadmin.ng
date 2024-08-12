package net.hostsharing.hsadminng.hs.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.mapper.Array;
import org.apache.commons.lang3.function.TriFunction;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.isArray;

@Getter
@RequiredArgsConstructor
public abstract class ValidatableProperty<P extends ValidatableProperty<?, ?>, T> {

    protected static final String[] KEY_ORDER_HEAD = Array.of("propertyName");
    protected static final String[] KEY_ORDER_TAIL = Array.of("required", "requiresAtLeastOneOf", "requiresAtMaxOneOf", "defaultValue", "readOnly", "writeOnce","writeOnly", "computed", "isTotalsValidator", "thresholdPercentage");
    protected static final String[] KEY_ORDER = Array.join(KEY_ORDER_HEAD, KEY_ORDER_TAIL);

    final Class<T> type;
    final String propertyName;

    @JsonIgnore
    private final String[] keyOrder;

    private Boolean required;
    private Set<String> requiresAtLeastOneOf;
    private Set<String> requiresAtMaxOneOf;
    private T defaultValue;

    protected enum ComputeMode {
        IN_INIT,
        IN_PREP,
        IN_REVAMP
    }

    @JsonIgnore
    private BiFunction<EntityManager, PropertiesProvider, T> computedBy;

    @Accessors(makeFinal = true, chain = true, fluent = false)
    private ComputeMode computed; // name 'computed' instead 'computeMode' for better readability in property description

    @Accessors(makeFinal = true, chain = true, fluent = false)
    private boolean readOnly;

    @Accessors(makeFinal = true, chain = true, fluent = false)
    private boolean writeOnly;

    @Accessors(makeFinal = true, chain = true, fluent = false)
    private boolean writeOnce;

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
        return self();
    }

    public P writeOnce() {
        this.writeOnce = true;
        return self();
    }

    public P readOnly() {
        this.readOnly = true;
        return self();
    }

    public P required() {
        required = TRUE;
        return self();
    }

    public P optional() {
        required = FALSE;
        return self();
    }

    public P requiresAtLeastOneOf(final String... propNames) {
        requiresAtLeastOneOf = new LinkedHashSet<>(List.of(propNames));
        return self();
    }

    public P requiresAtMaxOneOf(final String... propNames) {
        requiresAtMaxOneOf = new LinkedHashSet<>(List.of(propNames));
        return self();
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
        final TriFunction<HsBookingItemEntity, IntegerProperty<?>, Integer, List<String>> validator =
                (final HsBookingItemEntity entity, final IntegerProperty<?> prop, final Integer factor) -> {

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

    public ValidatableProperty<P, T> eachComprising(final int factor, final TriFunction<HsBookingItemEntity, IntegerProperty<?>, Integer, List<String>> validator) {
        if (asTotalLimitValidators == null) {
            asTotalLimitValidators = new ArrayList<>();
        }
        asTotalLimitValidators.add((final HsBookingItemEntity entity) -> validator.apply(entity, (IntegerProperty<?>)this,  factor));
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
            if (required == TRUE) {
                result.add(propertyName + "' is required but missing");
            }
            if (isWriteOnce() && propsProvider.isLoaded() && propsProvider.isPatched(propertyName) ) {
                result.add(propertyName + "' is write-once but got removed");
            }
            validateRequiresAtLeastOneOf(result, propsProvider);
        }
        if (propValue != null){
            validateRequiresAtMaxOneOf(result, propsProvider);

            if ( type.isInstance(propValue)) {
                //noinspection unchecked
                validate(result, (T) propValue, propsProvider);
            } else {
                result.add(propertyName + "' is expected to be of type " + type.getSimpleName() + ", " +
                        "but is of type " + propValue.getClass().getSimpleName());
            }
        }
        return result;
    }

    private void validateRequiresAtLeastOneOf(final ArrayList<String> result, final PropertiesProvider propsProvider) {
        if (requiresAtLeastOneOf != null ) {
            final var allPropNames = propsProvider.directProps().keySet();
            final var entriesWithValue = allPropNames.stream()
                    .filter(name -> requiresAtLeastOneOf.contains(name))
                    .count();
            if (entriesWithValue == 0) {
                result.add(propertyName + "' is required once in group " + requiresAtLeastOneOf + " but missing");
            }
        }
    }

    private void validateRequiresAtMaxOneOf(final ArrayList<String> result, final PropertiesProvider propsProvider) {
        if (requiresAtMaxOneOf != null) {
            final var allPropNames = propsProvider.directProps().keySet();
            final var entriesWithValue = allPropNames.stream()
                    .filter(name -> requiresAtMaxOneOf.contains(name))
                    .count();
            if (entriesWithValue > 1) {
                result.add(propertyName + "' is required at max once in group " + requiresAtMaxOneOf
                        + " but multiple properties are set");
            }
        }
    }

    protected void validate(final List<String> result, final T propValue, final PropertiesProvider propProvider) {
        if (isReadOnly() && propValue != null) {
            result.add(propertyName + "' is readonly but given as " + display(propValue));
        }
        if (isWriteOnce() && propProvider.isLoaded() && propValue != null && propProvider.isPatched(propertyName) ) {
            result.add(propertyName + "' is write-once but given as " + display(propValue));
        }
    }

    public void verifyConsistency(final Map.Entry<? extends Enum<?>, ?> typeDef) {
        if (isSpecPotentiallyComplete()) {
            throw new IllegalStateException(typeDef.getKey() + "[" + propertyName + "] not fully initialized, please call either .readOnly(), .required(), .optional(), .withDefault(...), .requiresAtLeastOneOf(...) or .requiresAtMaxOneOf(...)" );
        }
    }

    private boolean isSpecPotentiallyComplete() {
        return required == null && requiresAtLeastOneOf == null && requiresAtMaxOneOf == null && !readOnly && !writeOnly
                && defaultValue == null;
    }

    @SuppressWarnings("unchecked")
    public T getValue(final Map<String, Object> propValues) {
        return (T) Optional.ofNullable(propValues.get(propertyName)).orElse(defaultValue);
    }

    protected String display(final T propValue) {
        return propValue == null ? null : propValue.toString();
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
        if (isArray(value)) {
            return Arrays.stream((Object[])value).map(Object::toString).toList();
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

    public P initializedBy(final BiFunction<EntityManager, PropertiesProvider, T> compute) {
        return computedBy(ComputeMode.IN_INIT, compute);
    }

    public P renderedBy(final BiFunction<EntityManager, PropertiesProvider, T> compute) {
        return computedBy(ComputeMode.IN_REVAMP, compute);
    }

    protected P computedBy(final ComputeMode computeMode, final BiFunction<EntityManager, PropertiesProvider, T> compute) {
        this.computedBy = compute;
        this.computed = computeMode;
        return self();
    }

    public boolean isComputed(final ComputeMode computeMode) {
        return computed == computeMode;
    }

    public <E extends PropertiesProvider> T compute(final EntityManager em, final E entity) {
        return computedBy.apply(em, entity);
    }

    @Override
    public String toString() {
        return toOrderedMap().toString();
    }
}
