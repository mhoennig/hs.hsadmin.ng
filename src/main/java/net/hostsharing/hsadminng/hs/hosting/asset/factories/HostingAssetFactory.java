package net.hostsharing.hsadminng.hs.hosting.asset.factories;

import lombok.RequiredArgsConstructor;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsHostingAssetAutoInsertResource;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.hs.hosting.asset.validators.HostingAssetEntitySaveProcessor;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;

import java.util.UUID;

@RequiredArgsConstructor
abstract class HostingAssetFactory {

    final EntityManagerWrapper emw;
    final HsBookingItemRealEntity fromBookingItem;
    final HsHostingAssetAutoInsertResource asset;
    final StandardMapper standardMapper;

    protected abstract HsHostingAsset create();

    public String performSaveProcess() {
        try {
            final var newHostingAsset = create();
            persist(newHostingAsset);
            return null;
        } catch (final Exception e) {
            return e.getMessage();
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

    protected <T> T ref(final Class<T> entityClass, final UUID uuid) {
        return uuid != null ? emw.getReference(entityClass, uuid) : null;
    }
}
