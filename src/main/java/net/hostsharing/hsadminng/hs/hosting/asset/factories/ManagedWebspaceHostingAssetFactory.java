package net.hostsharing.hsadminng.hs.hosting.asset.factories;

import lombok.val;
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
    protected void validateRequest() {
        if (asset.getType() != HsHostingAssetTypeResource.MANAGED_WEBSPACE) {
            throw new ValidationException("requires MANAGED_WEBSPACE hosting asset, but got " +
                    Optional.of(asset)
                            .map(HsHostingAssetAutoInsertResource::getType)
                            .map(Enum::name)
                            .orElse(null));
        }
    }

    @Override
    protected HsHostingAsset create() {
        val managedWebspaceHostingAsset = StrictMapper.map(asset, HsHostingAssetRealEntity.class);
        managedWebspaceHostingAsset.setBookingItem(fromBookingItem);
        // a webspace whose booking item has no parent booking item is standalone, as on shared hosting
        Optional.ofNullable(fromBookingItem.getParentItem())
                .flatMap(parentItem -> emw.createQuery(
                                "SELECT asset FROM HsHostingAssetRealEntity asset WHERE asset.bookingItem.uuid=:bookingItemUuid",
                                HsHostingAssetRealEntity.class)
                        .setParameter("bookingItemUuid", parentItem.getUuid())
                        .setMaxResults(1)
                        .getResultList().stream().findFirst())
                .ifPresent(managedWebspaceHostingAsset::setParentAsset);

        return managedWebspaceHostingAsset;
    }

    @Override
    protected void persist(final HsHostingAsset newManagedWebspaceHostingAsset) {
        super.persist(newManagedWebspaceHostingAsset);
    }
}
