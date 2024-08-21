package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetPatchResource;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.KeyValueMap;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;

import jakarta.persistence.EntityManager;
import java.util.Optional;

public class HsHostingAssetEntityPatcher implements EntityPatcher<HsHostingAssetPatchResource> {

    private final EntityManager em;
    private final HsHostingAssetRbacEntity entity;

    public HsHostingAssetEntityPatcher(final EntityManager em, final HsHostingAssetRbacEntity entity) {
        this.em = em;
        this.entity = entity;
    }

    @Override
    public void apply(final HsHostingAssetPatchResource resource) {
        OptionalFromJson.of(resource.getCaption())
                .ifPresent(entity::setCaption);
        Optional.ofNullable(resource.getConfig())
                .ifPresent(r -> entity.getConfig().patch(KeyValueMap.from(resource.getConfig())));
        OptionalFromJson.of(resource.getAlarmContactUuid())
                // HOWTO: patch nullable JSON resource uuid to an ntity reference
                .ifPresent(newValue -> entity.setAlarmContact(
                            Optional.ofNullable(newValue)
                                .map(uuid -> em.getReference(HsOfficeContactRealEntity.class, newValue))
                                .orElse(null)));
    }
}
