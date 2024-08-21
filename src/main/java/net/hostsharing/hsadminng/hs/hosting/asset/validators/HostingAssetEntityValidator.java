package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItem;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.booking.item.validators.HsBookingItemEntityValidatorRegistry;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;
import net.hostsharing.hsadminng.hs.validation.ValidatableProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public abstract class HostingAssetEntityValidator extends HsEntityValidator<HsHostingAsset> {

    static final ValidatableProperty<?, ?>[] NO_EXTRA_PROPERTIES = new ValidatableProperty<?, ?>[0];

    private final ReferenceValidator<HsBookingItem, HsBookingItemType> bookingItemReferenceValidation;
    private final ReferenceValidator<HsHostingAsset, HsHostingAssetType> parentAssetReferenceValidation;
    private final ReferenceValidator<HsHostingAsset, HsHostingAssetType> assignedToAssetReferenceValidation;
    private final HostingAssetEntityValidator.AlarmContact alarmContactValidation;

    HostingAssetEntityValidator(
            final HsHostingAssetType assetType,
            final AlarmContact alarmContactValidation, // hostmaster alert address is implicitly added where needed
            final ValidatableProperty<?, ?>... properties) {
        super(properties);
        this.bookingItemReferenceValidation = new ReferenceValidator<>(
                assetType.bookingItemPolicy(),
                assetType.bookingItemTypes(),
                HsHostingAsset::getBookingItem,
                HsBookingItem::getType);
        this.parentAssetReferenceValidation = new ReferenceValidator<>(
                assetType.parentAssetPolicy(),
                assetType.parentAssetTypes(),
                HsHostingAsset::getParentAsset,
                HsHostingAsset::getType);
        this.assignedToAssetReferenceValidation = new ReferenceValidator<>(
                assetType.assignedToAssetPolicy(),
                assetType.assignedToAssetTypes(),
                HsHostingAsset::getAssignedToAsset,
                HsHostingAsset::getType);
        this.alarmContactValidation = alarmContactValidation;
    }

    @Override
    public List<String> validateEntity(final HsHostingAsset assetEntity) {
        return sequentiallyValidate(
                () -> validateEntityReferencesAndProperties(assetEntity),
                () -> validateIdentifierPattern(assetEntity)
        );
    }

    @Override
    public List<String> validateContext(final HsHostingAsset assetEntity) {
        return sequentiallyValidate(
                () -> optionallyValidate(assetEntity.getBookingItem()),
                () -> optionallyValidate(assetEntity.getParentAsset()),
                () -> validateAgainstSubEntities(assetEntity)
        );
    }

    private List<String> validateEntityReferencesAndProperties(final HsHostingAsset assetEntity) {
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
            final HsHostingAsset assetEntity,
            final String referenceFieldName,
            final BiFunction<HsHostingAsset, String, List<String>> validator) {
        return enrich(prefix(assetEntity.toShortString()), validator.apply(assetEntity, referenceFieldName));
    }

    private List<String> validateProperties(final HsHostingAsset assetEntity) {
        return enrich(prefix(assetEntity.toShortString(), "config"), super.validateProperties(assetEntity));
    }

    private static List<String> optionallyValidate(final HsHostingAsset assetEntity) {
        return assetEntity != null
                ? enrich(
                    prefix(assetEntity.toShortString(), "parentAsset"),
                    HostingAssetEntityValidatorRegistry.forType(assetEntity.getType()).validateContext(assetEntity))
                : emptyList();
    }

    private static List<String> optionallyValidate(final HsBookingItem bookingItem) {
        return bookingItem != null
                ? enrich(
                prefix(bookingItem.toShortString(), "bookingItem"),
                HsBookingItemEntityValidatorRegistry.forType(bookingItem.getType()).validateContext(bookingItem))
                : emptyList();
    }

    protected List<String> validateAgainstSubEntities(final HsHostingAsset assetEntity) {
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
            final HsHostingAsset hostingAsset,
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

    private List<String> validateIdentifierPattern(final HsHostingAsset assetEntity) {
        final var expectedIdentifierPattern = identifierPattern(assetEntity);
        if (assetEntity.getIdentifier() == null ||
                !expectedIdentifierPattern.matcher(assetEntity.getIdentifier()).matches()) {
            return List.of(
                    "'identifier' expected to match '" + expectedIdentifierPattern + "', but is '" + assetEntity.getIdentifier()
                            + "'");
        }
        return Collections.emptyList();
    }

    protected abstract Pattern identifierPattern(HsHostingAsset assetEntity);

    static class ReferenceValidator<S, T> {

        private final HsHostingAssetType.RelationPolicy policy;
        private final Set<T> referencedEntityTypes;
        private final Function<HsHostingAsset, S> referencedEntityGetter;
        private final Function<S, T> referencedEntityTypeGetter;

        public ReferenceValidator(
                final HsHostingAssetType.RelationPolicy policy,
                final Set<T> referencedEntityTypes,
                final Function<HsHostingAsset, S> referencedEntityGetter,
                final Function<S, T> referencedEntityTypeGetter) {
            this.policy = policy;
            this.referencedEntityTypes = referencedEntityTypes;
            this.referencedEntityGetter = referencedEntityGetter;
            this.referencedEntityTypeGetter = referencedEntityTypeGetter;
        }

        public ReferenceValidator(
                final HsHostingAssetType.RelationPolicy policy,
                final Function<HsHostingAsset, S> referencedEntityGetter) {
            this.policy = policy;
            this.referencedEntityTypes = Set.of();
            this.referencedEntityGetter = referencedEntityGetter;
            this.referencedEntityTypeGetter = e -> null;
        }

        List<String> validate(final HsHostingAsset assetEntity, final String referenceFieldName) {

            final var actualEntity = referencedEntityGetter.apply(assetEntity);
            final var actualEntityType = actualEntity != null ? referencedEntityTypeGetter.apply(actualEntity) : null;

            switch (policy) {
            case REQUIRED:
                if (!referencedEntityTypes.contains(actualEntityType)) {
                    return List.of(actualEntityType == null
                            ? referenceFieldName + "' must be of type " + toDisplay(referencedEntityTypes) + " but is null"
                            : referenceFieldName + "' must be of type " + toDisplay(referencedEntityTypes) + " but is of type " + actualEntityType);
                }
                break;
            case OPTIONAL:
                if (actualEntityType != null && !referencedEntityTypes.contains(actualEntityType)) {
                    return List.of(referenceFieldName + "' must be null or of type " + toDisplay(referencedEntityTypes) + " but is of type "
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

        private String toDisplay(final Set<T> referencedEntityTypes) {
            return referencedEntityTypes.stream().sorted().map(Object::toString).collect(Collectors.joining(" or "));
        }
    }

    static class AlarmContact extends ReferenceValidator<HsOfficeContactRealEntity, Enum<?>> {

        AlarmContact(final HsHostingAssetType.RelationPolicy policy) {
            super(policy, HsHostingAsset::getAlarmContact);
        }

        // hostmaster alert address is implicitly added where neccessary
        static AlarmContact isOptional() {
            return new AlarmContact(HsHostingAssetType.RelationPolicy.OPTIONAL);
        }
    }

}
