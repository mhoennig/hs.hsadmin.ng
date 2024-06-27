package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.booking.item.validators.HsBookingItemEntityValidatorRegistry;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;
import net.hostsharing.hsadminng.hs.validation.ValidatableProperty;

import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public abstract class HsHostingAssetEntityValidator extends HsEntityValidator<HsHostingAssetEntity> {

    static final ValidatableProperty<?>[] NO_EXTRA_PROPERTIES = new ValidatableProperty<?>[0];

    private final HsHostingAssetEntityValidator.BookingItem bookingItemValidation;
    private final HsHostingAssetEntityValidator.ParentAsset parentAssetValidation;
    private final HsHostingAssetEntityValidator.AssignedToAsset assignedToAssetValidation;
    private final HsHostingAssetEntityValidator.AlarmContact alarmContactValidation;

    HsHostingAssetEntityValidator(
            @NotNull final BookingItem bookingItemValidation,
            @NotNull final ParentAsset parentAssetValidation,
            @NotNull final AssignedToAsset assignedToAssetValidation,
            @NotNull final AlarmContact alarmContactValidation,
            final ValidatableProperty<?>... properties) {
        super(properties);
        this.bookingItemValidation = bookingItemValidation;
        this.parentAssetValidation = parentAssetValidation;
        this.assignedToAssetValidation = assignedToAssetValidation;
        this.alarmContactValidation = alarmContactValidation;
    }

    @Override
    public List<String> validate(final HsHostingAssetEntity assetEntity) {
        return sequentiallyValidate(
                () -> validateEntityReferencesAndProperties(assetEntity),
                () -> validateIdentifierPattern(assetEntity), // might need proper parentAsset or billingItem
                () -> optionallyValidate(assetEntity.getBookingItem()),
                () -> optionallyValidate(assetEntity.getParentAsset()),
                () -> validateAgainstSubEntities(assetEntity)
        );
    }

    private List<String> validateEntityReferencesAndProperties(final HsHostingAssetEntity assetEntity) {
        return Stream.of(
                    validateReferencedEntity(assetEntity, "bookingItem", bookingItemValidation::validate),
                    validateReferencedEntity(assetEntity, "parentAsset", parentAssetValidation::validate),
                    validateReferencedEntity(assetEntity, "assignedToAsset", assignedToAssetValidation::validate),
                    validateReferencedEntity(assetEntity, "alarmContact", alarmContactValidation::validate),
                    validateProperties(assetEntity))
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> validateReferencedEntity(
            final HsHostingAssetEntity assetEntity,
            final String referenceFieldName,
            final BiFunction<HsHostingAssetEntity, String, List<String>> validator) {
        return enrich(prefix(assetEntity.toShortString()), validator.apply(assetEntity, referenceFieldName));
    }

    private List<String> validateProperties(final HsHostingAssetEntity assetEntity) {
        return enrich(prefix(assetEntity.toShortString(), "config"), super.validateProperties(assetEntity));
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

    // TODO.test: check, if there are any hosting assets which need this validation at all
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

    private List<String> validateIdentifierPattern(final HsHostingAssetEntity assetEntity) {
        final var expectedIdentifierPattern = identifierPattern(assetEntity);
        if (assetEntity.getIdentifier() == null ||
                !expectedIdentifierPattern.matcher(assetEntity.getIdentifier()).matches()) {
            return List.of("'identifier' expected to match '"+expectedIdentifierPattern+"', but is '" + assetEntity.getIdentifier() + "'");
        }
        return Collections.emptyList();
    }

    protected abstract Pattern identifierPattern(HsHostingAssetEntity assetEntity);

    static abstract class ReferenceValidator<S, T> {

        private final Policy policy;
        private final T subEntityType;
        private final Function<HsHostingAssetEntity, S> subEntityGetter;
        private final Function<S,T> subEntityTypeGetter;

        public ReferenceValidator(
                final Policy policy,
                final T subEntityType,
                final Function<HsHostingAssetEntity, S> subEntityGetter,
                final Function<S, T> subEntityTypeGetter) {
            this.policy = policy;
            this.subEntityType = subEntityType;
            this.subEntityGetter = subEntityGetter;
            this.subEntityTypeGetter = subEntityTypeGetter;
        }

        public ReferenceValidator(
                final Policy policy,
                final Function<HsHostingAssetEntity, S> subEntityGetter) {
            this.policy = policy;
            this.subEntityType = null;
            this.subEntityGetter = subEntityGetter;
            this.subEntityTypeGetter = e -> null;
        }

        enum Policy {
            OPTIONAL, FORBIDDEN, REQUIRED
        }

        List<String> validate(final HsHostingAssetEntity assetEntity, final String referenceFieldName) {

            final var subEntity = subEntityGetter.apply(assetEntity);
            if (policy == Policy.REQUIRED && subEntity == null) {
                return List.of(referenceFieldName + "' must not be null but is null");
            }
            if (policy == Policy.FORBIDDEN && subEntity != null) {
                return List.of(referenceFieldName + "' must be null but is set to "+ assetEntity.getBookingItem().toShortString());
            }
            final var subItemType = subEntity != null ? subEntityTypeGetter.apply(subEntity) : null;
            if (subEntityType != null && subItemType != subEntityType) {
                return List.of(referenceFieldName + "' must be of type " + subEntityType + " but is of type " + subItemType);
            }
            return emptyList();
        }
    }

    static class BookingItem extends ReferenceValidator<HsBookingItemEntity, HsBookingItemType> {

        BookingItem(final Policy policy, final HsBookingItemType bookingItemType) {
            super(policy, bookingItemType, HsHostingAssetEntity::getBookingItem, HsBookingItemEntity::getType);
        }

        static BookingItem mustBeNull() {
            return new BookingItem(Policy.FORBIDDEN, null);
        }

        static BookingItem mustBeOfType(final HsBookingItemType hsBookingItemType) {
            return new BookingItem(Policy.REQUIRED, hsBookingItemType);
        }
    }

    static class ParentAsset extends ReferenceValidator<HsHostingAssetEntity, HsHostingAssetType> {

        ParentAsset(final ReferenceValidator.Policy policy, final HsHostingAssetType parentAssetType) {
            super(policy, parentAssetType, HsHostingAssetEntity::getParentAsset, HsHostingAssetEntity::getType);
        }

        static ParentAsset mustBeNull() {
            return new ParentAsset(Policy.FORBIDDEN, null);
        }

        static ParentAsset mustBeOfType(final HsHostingAssetType hostingAssetType) {
            return new ParentAsset(Policy.REQUIRED, hostingAssetType);
        }

        static ParentAsset mustBeNullOrOfType(final HsHostingAssetType hostingAssetType) {
            return new ParentAsset(Policy.OPTIONAL, hostingAssetType);
        }
    }

    static class AssignedToAsset extends ReferenceValidator<HsHostingAssetEntity, HsHostingAssetType> {

        AssignedToAsset(final ReferenceValidator.Policy policy, final HsHostingAssetType assignedToAssetType) {
            super(policy, assignedToAssetType, HsHostingAssetEntity::getAssignedToAsset, HsHostingAssetEntity::getType);
        }

        static AssignedToAsset mustBeNull() {
            return new AssignedToAsset(Policy.FORBIDDEN, null);
        }
    }

    static class AlarmContact extends ReferenceValidator<HsOfficeContactEntity, Enum<?>> {

        AlarmContact(final ReferenceValidator.Policy policy) {
            super(policy, HsHostingAssetEntity::getAlarmContact);
        }

        static AlarmContact isOptional() {
            return new AlarmContact(Policy.OPTIONAL);
        }
    }

}
