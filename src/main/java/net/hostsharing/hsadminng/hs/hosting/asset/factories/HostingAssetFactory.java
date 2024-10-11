package net.hostsharing.hsadminng.hs.hosting.asset.factories;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsHostingAssetAutoInsertResource;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.hs.hosting.asset.validators.HostingAssetEntitySaveProcessor;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;


@RequiredArgsConstructor
abstract class HostingAssetFactory {

    final EntityManagerWrapper emw;
    final HsBookingItemRealEntity fromBookingItem;
    final HsHostingAssetAutoInsertResource asset;
    final StandardMapper standardMapper;

    protected abstract HsHostingAsset create();

    public String createAndPersist() {
        try {
            final HsHostingAsset newHostingAsset = create();
            persist(newHostingAsset);
            return null;
        } catch (final ValidationException exc) {
            return exc.getMessage();
        }
    }

    protected void persist(final HsHostingAsset newHostingAsset) {
        new HostingAssetEntitySaveProcessor(emw, newHostingAsset)
                .preprocessEntity()
                .validateEntity()
                .prepareForSave()
                .save()
                .validateContext();
    }
}
