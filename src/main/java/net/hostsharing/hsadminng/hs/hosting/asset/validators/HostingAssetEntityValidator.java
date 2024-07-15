package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.booking.item.validators.HsBookingItemEntityValidatorRegistry;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;
import net.hostsharing.hsadminng.hs.validation.ValidatableProperty;

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

public abstract class HostingAssetEntityValidator extends HsEntityValidator<HsHostingAssetEntity> {

    static final ValidatableProperty<?, ?>[] NO_EXTRA_PROPERTIES = new ValidatableProperty<?, ?>[0];

    private final ReferenceValidator<HsBookingItemEntity, HsBookingItemType> bookingItemReferenceValidation;
    private final ReferenceValidator<HsHostingAssetEntity, HsHostingAssetType> parentAssetReferenceValidation;
    private final ReferenceValidator<HsHostingAssetEntity, HsHostingAssetType> assignedToAssetReferenceValidation;
    private final HostingAssetEntityValidator.AlarmContact alarmContactValidation;

    HostingAssetEntityValidator(
            final HsHostingAssetType assetType,
            final AlarmContact alarmContactValidation, // hostmaster alert address is implicitly added where needed
            final ValidatableProperty<?, ?>... properties) {
        super(properties);
        this.bookingItemReferenceValidation = new ReferenceValidator<>(
                assetType.bookingItemPolicy(),
                assetType.bookingItemType(),
                HsHostingAssetEntity::getBookingItem,
                HsBookingItemEntity::getType);
        this.parentAssetReferenceValidation = new ReferenceValidator<>(
                assetType.parentAssetPolicy(),
                assetType.parentAssetType(),
                HsHostingAssetEntity::getParentAsset,
                HsHostingAssetEntity::getType);
        this.assignedToAssetReferenceValidation = new ReferenceValidator<>(
                assetType.assignedToAssetPolicy(),
                assetType.assignedToAssetType(),
                HsHostingAssetEntity::getAssignedToAsset,
                HsHostingAssetEntity::getType);
        this.alarmContactValidation = alarmContactValidation;
    }

    @Override
    public List<String> validateEntity(final HsHostingAssetEntity assetEntity) {
        return sequentiallyValidate(
                () -> validateEntityReferencesAndProperties(assetEntity),
                () -> validateIdentifierPattern(assetEntity)
        );
    }

    @Override
    public List<String> validateContext(final HsHostingAssetEntity assetEntity) {
        return sequentiallyValidate(
                () -> optionallyValidate(assetEntity.getBookingItem()),
                () -> optionallyValidate(assetEntity.getParentAsset()),
                () -> validateAgainstSubEntities(assetEntity)
        );
    }

    private List<String> validateEntityReferencesAndProperties(final HsHostingAssetEntity assetEntity) {
        return Stream.of(
                        validateReferencedEntity(assetEntity, "bookingItem", bookingItemReferenceValidation::validate),
                        validateReferencedEntity(assetEntity, "parentAsset", parentAssetReferenceValidation::validate),
                        validateReferencedEntity(assetEntity, "assignedToAsset", assignedToAssetReferenceValidation::validate),
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
                ? enrich(
                    prefix(assetEntity.toShortString(), "parentAsset"),
                    HostingAssetEntityValidatorRegistry.forType(assetEntity.getType()).validateContext(assetEntity))
                : emptyList();
    }

    private static List<String> optionallyValidate(final HsBookingItemEntity bookingItem) {
        return bookingItem != null
                ? enrich(
                prefix(bookingItem.toShortString(), "bookingItem"),
                HsBookingItemEntityValidatorRegistry.forType(bookingItem.getType()).validateContext(bookingItem))
                : emptyList();
    }

    protected List<String> validateAgainstSubEntities(final HsHostingAssetEntity assetEntity) {
        return enrich(
                prefix(assetEntity.toShortString(), "config"),
                stream(propertyValidators)
                        .filter(ValidatableProperty::isTotalsValidator)
                        .map(prop -> validateMaxTotalValue(assetEntity, prop))
                        .filter(Objects::nonNull)
                        .toList());
    }

    // TODO.test: check, if there are any hosting assets which need this validation at all
    private String validateMaxTotalValue(
            final HsHostingAssetEntity hostingAsset,
            final ValidatableProperty<?, ?> propDef) {
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
            return List.of(
                    "'identifier' expected to match '" + expectedIdentifierPattern + "', but is '" + assetEntity.getIdentifier()
                            + "'");
        }
        return Collections.emptyList();
    }

    protected abstract Pattern identifierPattern(HsHostingAssetEntity assetEntity);

    static class ReferenceValidator<S, T> {

        private final HsHostingAssetType.RelationPolicy policy;
        private final T referencedEntityType;
        private final Function<HsHostingAssetEntity, S> referencedEntityGetter;
        private final Function<S, T> referencedEntityTypeGetter;

        public ReferenceValidator(
                final HsHostingAssetType.RelationPolicy policy,
                final T subEntityType,
                final Function<HsHostingAssetEntity, S> referencedEntityGetter,
                final Function<S, T> referencedEntityTypeGetter) {
            this.policy = policy;
            this.referencedEntityType = subEntityType;
            this.referencedEntityGetter = referencedEntityGetter;
            this.referencedEntityTypeGetter = referencedEntityTypeGetter;
        }

        public ReferenceValidator(
                final HsHostingAssetType.RelationPolicy policy,
                final Function<HsHostingAssetEntity, S> referencedEntityGetter) {
            this.policy = policy;
            this.referencedEntityType = null;
            this.referencedEntityGetter = referencedEntityGetter;
            this.referencedEntityTypeGetter = e -> null;
        }

        List<String> validate(final HsHostingAssetEntity assetEntity, final String referenceFieldName) {

            final var actualEntity = referencedEntityGetter.apply(assetEntity);
            final var actualEntityType = actualEntity != null ? referencedEntityTypeGetter.apply(actualEntity) : null;

            switch (policy) {
            case REQUIRED:
                if (actualEntityType != referencedEntityType) {
                    return List.of(actualEntityType == null
                            ? referenceFieldName + "' must be of type " + referencedEntityType + " but is null"
                            : referenceFieldName + "' must be of type " + referencedEntityType + " but is of type " + actualEntityType);
                }
                break;
            case OPTIONAL:
                if (actualEntityType != null && actualEntityType != referencedEntityType) {
                    return List.of(referenceFieldName + "' must be null or of type " + referencedEntityType + " but is of type "
                            + actualEntityType);
                }
                break;
            case FORBIDDEN:
                if (actualEntityType != null) {
                    return List.of(referenceFieldName + "' must be null but is of type " + actualEntityType);
                }
                break;
            }
            return emptyList();
        }
    }

    static class AlarmContact extends ReferenceValidator<HsOfficeContactEntity, Enum<?>> {

        AlarmContact(final HsHostingAssetType.RelationPolicy policy) {
            super(policy, HsHostingAssetEntity::getAlarmContact);
        }

        // hostmaster alert address is implicitly added where neccessary
        static AlarmContact isOptional() {
            return new AlarmContact(HsHostingAssetType.RelationPolicy.OPTIONAL);
        }
    }

}
