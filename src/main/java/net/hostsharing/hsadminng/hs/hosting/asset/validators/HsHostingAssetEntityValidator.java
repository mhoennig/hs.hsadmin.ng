package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.validators.HsBookingItemEntityValidatorRegistry;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;
import net.hostsharing.hsadminng.hs.validation.ValidatableProperty;

import java.util.List;
import java.util.Objects;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public class HsHostingAssetEntityValidator extends HsEntityValidator<HsHostingAssetEntity> {

    public HsHostingAssetEntityValidator(final ValidatableProperty<?>... properties) {
        super(properties);
    }


    @Override
    public List<String> validate(final HsHostingAssetEntity assetEntity) {
        return sequentiallyValidate(
                () -> validateProperties(assetEntity),
                () -> optionallyValidate(assetEntity.getBookingItem()),
                () -> optionallyValidate(assetEntity.getParentAsset()),
                () -> validateAgainstSubEntities(assetEntity)
        );
    }

    private List<String> validateProperties(final HsHostingAssetEntity assetEntity) {
        return enrich(prefix(assetEntity.toShortString(), "config"), validateProperties(assetEntity.getConfig()));
    }

    private static List<String> optionallyValidate(final HsHostingAssetEntity assetEntity) {
        return assetEntity != null
                ? enrich(prefix(assetEntity.toShortString(), "parentAsset"),
                        HsHostingAssetEntityValidatorRegistry.forType(assetEntity.getType()).validate(assetEntity))
                : emptyList();
    }

    private static List<String> optionallyValidate(final HsBookingItemEntity bookingItem) {
        return bookingItem != null
                ? enrich(prefix(bookingItem.toShortString(), "bookingItem"),
                    HsBookingItemEntityValidatorRegistry.doValidate(bookingItem))
                : emptyList();
    }

    protected List<String> validateAgainstSubEntities(final HsHostingAssetEntity assetEntity) {
        return enrich(prefix(assetEntity.toShortString(), "config"),
                stream(propertyValidators)
                    .filter(ValidatableProperty::isTotalsValidator)
                    .map(prop -> validateMaxTotalValue(assetEntity, prop))
                    .filter(Objects::nonNull)
                    .toList());
    }

    private String validateMaxTotalValue(
            final HsHostingAssetEntity hostingAsset,
            final ValidatableProperty<?> propDef) {
        final var propName = propDef.propertyName();
        final var propUnit = ofNullable(propDef.unit()).map(u -> " " + u).orElse("");
        final var totalValue = ofNullable(hostingAsset.getSubHostingAssets()).orElse(emptyList())
                .stream()
                .map(subItem -> propDef.getValue(subItem.getConfig()))
                .map(HsEntityValidator::toIntegerWithDefault0)
                .reduce(0, Integer::sum);
        final var maxValue = getIntegerValueWithDefault0(propDef, hostingAsset.getConfig());
        return totalValue > maxValue
                ? "%s' maximum total is %d%s, but actual total %s is %d%s".formatted(
                propName, maxValue, propUnit, propName, totalValue, propUnit)
                : null;
    }
}
