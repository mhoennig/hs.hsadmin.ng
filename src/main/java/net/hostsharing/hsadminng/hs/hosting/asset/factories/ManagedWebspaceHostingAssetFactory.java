package net.hostsharing.hsadminng.hs.hosting.asset.factories;

import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsHostingAssetAutoInsertResource;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsHostingAssetTypeResource;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;

import jakarta.validation.ValidationException;

import java.util.Optional;


public class ManagedWebspaceHostingAssetFactory extends HostingAssetFactory {

    public ManagedWebspaceHostingAssetFactory(
            final EntityManagerWrapper emw,
            final HsBookingItemRealEntity newBookingItemRealEntity,
            final HsHostingAssetAutoInsertResource asset,
            final StrictMapper StrictMapper) {
        super(emw, newBookingItemRealEntity, asset, StrictMapper);
    }

    @Override
    protected HsHostingAsset create() {
        if (asset.getType() != HsHostingAssetTypeResource.MANAGED_WEBSPACE) {
            throw new ValidationException("requires MANAGED_WEBSPACE hosting asset, but got " +
                    Optional.of(asset)
                            .map(HsHostingAssetAutoInsertResource::getType)
                            .map(Enum::name)
                            .orElse(null));
        }
        final var managedWebspaceHostingAsset = StrictMapper.map(asset, HsHostingAssetRealEntity.class);
        managedWebspaceHostingAsset.setBookingItem(fromBookingItem);
        emw.createQuery(
                "SELECT asset FROM HsHostingAssetRealEntity asset WHERE asset.bookingItem.uuid=:bookingItemUuid",
                HsHostingAssetRealEntity.class)
                .setParameter("bookingItemUuid", fromBookingItem.getParentItem().getUuid())
                .getResultStream().findFirst()
                .ifPresent(managedWebspaceHostingAsset::setParentAsset);

        return managedWebspaceHostingAsset;
    }

    @Override
    protected void persist(final HsHostingAsset newManagedWebspaceHostingAsset) {
        super.persist(newManagedWebspaceHostingAsset);
    }
}
