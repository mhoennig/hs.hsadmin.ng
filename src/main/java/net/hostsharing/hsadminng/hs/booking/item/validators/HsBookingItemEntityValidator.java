package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;
import net.hostsharing.hsadminng.hs.validation.ValidatableProperty;
import org.apache.commons.lang3.BooleanUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public class HsBookingItemEntityValidator extends HsEntityValidator<HsBookingItemEntity> {

    public HsBookingItemEntityValidator(final ValidatableProperty<?, ?>... properties) {
        super(properties);
    }

    @Override
    public List<String> validateEntity(final HsBookingItemEntity bookingItem) {
        // TODO.impl: HsBookingItemType could do this similar to HsHostingAssetType
        if ( bookingItem.getParentItem() == null && bookingItem.getProject() == null) {
            return List.of(bookingItem + ".'parentItem' or .'project' expected to be set, but both are null");
        }
        return enrich(prefix(bookingItem.toShortString(), "resources"), super.validateProperties(bookingItem));
    }

    @Override
    public List<String> validateContext(final HsBookingItemEntity bookingItem) {
        return sequentiallyValidate(
                () -> optionallyValidate(bookingItem.getParentItem()),
                () -> validateAgainstSubEntities(bookingItem)
        );
    }

    private static List<String> optionallyValidate(final HsBookingItemEntity bookingItem) {
           return bookingItem != null
                   ? enrich(prefix(bookingItem.toShortString(), ""),
                        HsBookingItemEntityValidatorRegistry.forType(bookingItem.getType()).validateContext(bookingItem))
                   : emptyList();
    }

    protected List<String> validateAgainstSubEntities(final HsBookingItemEntity bookingItem) {
        return enrich(prefix(bookingItem.toShortString(), "resources"),
                    Stream.concat(
                        stream(propertyValidators)
                                .map(propDef -> propDef.validateTotals(bookingItem))
                                .flatMap(Collection::stream),
                        stream(propertyValidators)
                                .filter(ValidatableProperty::isTotalsValidator)
                                .map(prop -> validateMaxTotalValue(bookingItem, prop))
                ).filter(Objects::nonNull).toList());
    }

    // TODO.refa: convert into generic shape like multi-options validator
    private static String validateMaxTotalValue(
            final HsBookingItemEntity bookingItem,
            final ValidatableProperty<?, ?> propDef) {
        final var propName = propDef.propertyName();
        final var propUnit = ofNullable(propDef.unit()).map(u -> " " + u).orElse("");
        final var totalValue = ofNullable(bookingItem.getSubBookingItems()).orElse(emptyList())
                .stream()
                .map(subItem -> propDef.getValue(subItem.getResources()))
                .map(HsBookingItemEntityValidator::convertBooleanToInteger)
                .map(HsBookingItemEntityValidator::toIntegerWithDefault0)
                .reduce(0, Integer::sum);
        final var maxValue = getIntegerValueWithDefault0(propDef, bookingItem.getResources());
        if (propDef.thresholdPercentage() != null ) {
            return totalValue > (maxValue * propDef.thresholdPercentage() / 100)
                    ? "%s' maximum total is %d%s, but actual total %s is %d%s, which exceeds threshold of %d%%"
                        .formatted(propName, maxValue, propUnit, propName, totalValue, propUnit, propDef.thresholdPercentage())
                    : null;
        } else {
            return totalValue > maxValue
                    ? "%s' maximum total is %d%s, but actual total %s is %d%s"
                    .formatted(propName, maxValue, propUnit, propName, totalValue, propUnit)
                    : null;
        }
    }

    private static Object convertBooleanToInteger(final Object value) {
        return value instanceof Boolean ? BooleanUtils.toInteger((Boolean)value) : value;
    }
}
