package net.hostsharing.hsadminng.hs.hosting.asset.factories;

import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsHostingAssetAutoInsertResource;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsHostingAssetSubInsertResource;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsHostingAssetTypeResource;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.lambda.Reducer;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;

import jakarta.validation.ValidationException;
import java.net.IDN;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_DNS_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_HTTP_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_MBOX_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_SMTP_SETUP;

public class DomainSetupHostingAssetFactory extends HostingAssetFactory {

    public DomainSetupHostingAssetFactory(
            final EntityManagerWrapper emw,
            final HsBookingItemRealEntity newBookingItemRealEntity,
            final HsHostingAssetAutoInsertResource asset,
            final StandardMapper standardMapper) {
        super(emw, newBookingItemRealEntity, asset, standardMapper);
    }

    @Override
    protected HsHostingAsset create() {
        final var domainSetupAsset = createDomainSetupAsset(getDomainName());
        final var subHostingAssets = domainSetupAsset.getSubHostingAssets();

        // TODO.legacy: as long as we need to be compatible, we always do all technical domain-setups

        final var domainHttpSetupAssetResource = findSubHostingAssetResource(HsHostingAssetTypeResource.DOMAIN_HTTP_SETUP);
        final var assignedToUnixUserAssetEntity = domainHttpSetupAssetResource
                .map(HsHostingAssetSubInsertResource::getAssignedToAssetUuid)
                .map(uuid -> emw.find(HsHostingAssetRealEntity.class, uuid))
                .orElseThrow(() -> new ValidationException("DOMAIN_HTTP_SETUP subAsset with assignedToAssetUuid required in compatibility mode"));

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
                .alarmContact(ref(HsOfficeContactRealEntity.class, asset.getAlarmContactUuid()))
                // the sub-hosting-assets get added later
                .build();
    }

    private HsHostingAssetRealEntity createDomainSubSetupAssetEntity(
            final HsHostingAssetRealEntity domainSetupAsset,
            final HsHostingAssetType subAssetType,
            final Function<HsHostingAssetRealEntity.HsHostingAssetRealEntityBuilder<?, ?>, HsHostingAssetRealEntity.HsHostingAssetRealEntityBuilder<?, ?>> builderTransformer) {
        final var resourceType = HsHostingAssetTypeResource.valueOf(subAssetType.name());

        final var subAssetResourceOptional = findSubHostingAssetResource(resourceType);

        subAssetResourceOptional.ifPresentOrElse(
                subAssetResource -> verifyNotOverspecified(subAssetResource),
                () -> { throw new ValidationException("sub-asset of type " + resourceType.name() + " required in legacy mode, but missing"); }
        );

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
}
