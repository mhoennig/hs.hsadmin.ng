package net.hostsharing.hsadminng.hs.hosting.asset.factories;

import lombok.val;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsHostingAssetAutoInsertResource;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsHostingAssetSubInsertResource;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsHostingAssetTypeResource;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRbacRepository;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRbacRepository;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.lambda.Reducer;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.mapper.ToStringConverter;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;

import jakarta.validation.ValidationException;
import java.net.IDN;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_DNS_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_HTTP_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_MBOX_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_SMTP_SETUP;

public class DomainSetupHostingAssetFactory extends HostingAssetFactory {

    private static final List<HsHostingAssetType> REQUIRED_SUB_SETUP_TYPES =
            List.of(DOMAIN_DNS_SETUP, DOMAIN_HTTP_SETUP, DOMAIN_MBOX_SETUP, DOMAIN_SMTP_SETUP);

    private final HsOfficeContactRbacRepository rbacContactRepo;
    private final HsHostingAssetRbacRepository rbacAssetRepo;

    public DomainSetupHostingAssetFactory(
            final EntityManagerWrapper emw,
            final HsBookingItemRealEntity newBookingItemRealEntity,
            final HsHostingAssetAutoInsertResource asset,
            final StrictMapper StrictMapper,
            final HsOfficeContactRbacRepository rbacContactRepo,
            final HsHostingAssetRbacRepository rbacAssetRepo) {
        super(emw, newBookingItemRealEntity, asset, StrictMapper);
        this.rbacContactRepo = rbacContactRepo;
        this.rbacAssetRepo = rbacAssetRepo;
    }

    @Override
    protected void validateRequest() {
        // TODO.legacy: drop this all-or-nothing rule once the backend manages the DNS itself
        //  instead of the legacy file system, then sub-setups get assigned via the hosting-asset API
        if (getSubHostingAssetResources() == null || getSubHostingAssetResources().isEmpty()) {
            throw new ValidationException(
                    "'hostingAsset.subHostingAssets' required in compatibility mode, with sub-assets of type "
                            + requiredSubSetupTypes() + " (DOMAIN_HTTP_SETUP with assignedToAsset.uuid)");
        }
        REQUIRED_SUB_SETUP_TYPES.forEach(requiredSubSetupType ->
                findSubHostingAssetResource(resourceTypeOf(requiredSubSetupType)).ifPresentOrElse(
                        this::verifyNotOverspecified,
                        () -> {
                            throw new ValidationException(
                                    "sub-asset of type " + requiredSubSetupType + " required in legacy mode, but missing");
                        })
        );
        findSubHostingAssetResource(resourceTypeOf(DOMAIN_HTTP_SETUP))
                .map(HsHostingAssetSubInsertResource::getAssignedToAssetUuid)
                .orElseThrow(() -> new ValidationException(
                        "DOMAIN_HTTP_SETUP subAsset with assignedToAsset.uuid required in compatibility mode"));
    }

    @Override
    protected HsHostingAsset create() {
        final var domainSetupAsset = createDomainSetupAsset(getDomainName());
        final var subHostingAssets = domainSetupAsset.getSubHostingAssets();

        // TODO.legacy: as long as we need to be compatible, we always do all technical domain-setups

        final var assignedToUnixUserAssetEntity = findSubHostingAssetResource(resourceTypeOf(DOMAIN_HTTP_SETUP))
                .map(HsHostingAssetSubInsertResource::getAssignedToAssetUuid)
                // via the restricted view, so an invisible UNIX_USER behaves like a missing one (A-02)
                .flatMap(rbacAssetRepo::findVisibleByUuid)
                .orElseThrow(() -> new ValidationException(
                        "DOMAIN_HTTP_SETUP subAsset's assignedToAsset.uuid not found"));

        subHostingAssets.add(
                createDomainSubSetupAssetEntity(
                        domainSetupAsset,
                        DOMAIN_HTTP_SETUP,
                        builder -> builder
                                .assignedToAsset(assignedToUnixUserAssetEntity)
                                .identifier(getDomainName() + "|HTTP")
                                .caption("HTTP-Setup für " + IDN.toUnicode(getDomainName())))
        );

        // Do not add to subHostingAssets in compatibility mode, in this case, DNS setup works via file system.
        // The entity is created just for validation purposes.
        createDomainSubSetupAssetEntity(
                domainSetupAsset,
                DOMAIN_DNS_SETUP,
                builder -> builder
                        .assignedToAsset(assignedToUnixUserAssetEntity.getParentAsset())
                        .identifier(getDomainName() + "|DNS")
                        .caption("DNS-Setup für " + IDN.toUnicode(getDomainName())));

        subHostingAssets.add(
            createDomainSubSetupAssetEntity(
                    domainSetupAsset,
                    DOMAIN_MBOX_SETUP,
                    builder -> builder
                            .assignedToAsset(assignedToUnixUserAssetEntity.getParentAsset())
                            .identifier(getDomainName() + "|MBOX")
                            .caption("MBOX-Setup für " + IDN.toUnicode(getDomainName())))
        );

        subHostingAssets.add(
            createDomainSubSetupAssetEntity(
                    domainSetupAsset,
                    DOMAIN_SMTP_SETUP,
                    builder -> builder
                            .assignedToAsset(assignedToUnixUserAssetEntity.getParentAsset())
                            .identifier(getDomainName() + "|SMTP")
                            .caption("SMTP-Setup für " + IDN.toUnicode(getDomainName())))
        );

        return domainSetupAsset;
    }

    private HsHostingAssetRealEntity createDomainSetupAsset(final String domainName) {
        return HsHostingAssetRealEntity.builder()
                .bookingItem(fromBookingItem)
                .type(HsHostingAssetType.DOMAIN_SETUP)
                .identifier(domainName)
                .caption(asset.getCaption() != null ? asset.getCaption() : domainName)
                .alarmContact(visibleAlarmContactOrNull())
                // the sub-hosting-assets get added later
                .build();
    }

    private HsHostingAssetRealEntity createDomainSubSetupAssetEntity(
            final HsHostingAssetRealEntity domainSetupAsset,
            final HsHostingAssetType subAssetType,
            final Function<HsHostingAssetRealEntity.HsHostingAssetRealEntityBuilder<?, ?>, HsHostingAssetRealEntity.HsHostingAssetRealEntityBuilder<?, ?>> builderTransformer) {
        return builderTransformer.apply(
                        HsHostingAssetRealEntity.builder()
                                .type(subAssetType)
                                .parentAsset(domainSetupAsset))
                .build();
    }

    private Optional<HsHostingAssetSubInsertResource> findSubHostingAssetResource(final HsHostingAssetTypeResource resourceType) {
        return getSubHostingAssetResources().stream()
                .filter(ha -> ha.getType() == resourceType)
                .reduce(Reducer::toSingleElement);
    }

    // TODO.legacy: while we need to stay compatible, only default values can be used, thus only the type can be specified
    private void verifyNotOverspecified(final HsHostingAssetSubInsertResource givenSubAssetResource) {
        final var convert = new ToStringConverter().ignoring("assignedToAssetUuid");
        final var expectedSubAssetResource = new HsHostingAssetSubInsertResource();
        expectedSubAssetResource.setType(givenSubAssetResource.getType());
        if ( !convert.from(givenSubAssetResource).equals(convert.from(expectedSubAssetResource)) ) {
            throw new ValidationException("sub asset " + givenSubAssetResource.getType() + " is over-specified, in compatibility mode, only default values allowed");
        }

    }

    private static String requiredSubSetupTypes() {
        return REQUIRED_SUB_SETUP_TYPES.stream().map(Enum::name).collect(joining(", "));
    }

    private static HsHostingAssetTypeResource resourceTypeOf(final HsHostingAssetType subSetupType) {
        return HsHostingAssetTypeResource.valueOf(subSetupType.name());
    }

    private String getDomainName() {
        return asset.getIdentifier();
    }

    private List<HsHostingAssetSubInsertResource> getSubHostingAssetResources() {
        return asset.getSubHostingAssets();
    }

    @Override
    protected void persist(final HsHostingAsset newHostingAsset) {
        super.persist(newHostingAsset);
        newHostingAsset.getSubHostingAssets().forEach(super::persist);
    }

    // via the restricted view, so an invisible foreign contact cannot become the alarm-contact (A-02)
    private HsOfficeContactRealEntity visibleAlarmContactOrNull() {
        val alarmContactUuid = asset.getAlarmContactUuid();
        return alarmContactUuid == null ? null
                : rbacContactRepo.findVisibleByUuid(alarmContactUuid).orElseThrow(
                        () -> new ValidationException("cannot find Contact by alarmContact.uuid: " + alarmContactUuid));
    }
}
