package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetPatchResource;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRbacRepository;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.KeyValueMap;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

public class HsHostingAssetEntityPatcher implements EntityPatcher<HsHostingAssetPatchResource> {

    private final HsOfficeContactRbacRepository rbacContactRepo;
    private final HsHostingAssetRbacRepository rbacAssetRepo;
    private final HsHostingAssetRbacEntity entity;

    public HsHostingAssetEntityPatcher(
            final HsOfficeContactRbacRepository rbacContactRepo,
            final HsHostingAssetRbacRepository rbacAssetRepo,
            final HsHostingAssetRbacEntity entity) {
        this.rbacContactRepo = rbacContactRepo;
        this.rbacAssetRepo = rbacAssetRepo;
        this.entity = entity;
    }

    @Override
    public void apply(final HsHostingAssetPatchResource resource) {
        OptionalFromJson.of(resource.getCaption())
                .ifPresent(entity::setCaption);
        Optional.ofNullable(resource.getConfig())
                .ifPresent(r -> entity.getConfig().patch(KeyValueMap.from("config", resource.getConfig())));
        OptionalFromJson.of(resource.getAlarmContactUuid())
                // HOWTO: patch nullable JSON resource uuid to an entity reference
                .ifPresent(newValue -> entity.setAlarmContact(visibleContactOrNull(newValue)));
        OptionalFromJson.of(resource.getAssignedToAssetUuid())
                .ifPresent(newValue -> entity.setAssignedToAsset(visibleAssetOrNull(newValue)));
    }

    private HsOfficeContactRealEntity visibleContactOrNull(final UUID contactUuid) {
        return contactUuid == null ? null
                : rbacContactRepo.findVisibleByUuid(contactUuid).orElseThrow(
                        () -> new NoSuchElementException("cannot find Contact by alarmContact.uuid: " + contactUuid));
    }

    private HsHostingAssetRealEntity visibleAssetOrNull(final UUID assetUuid) {
        return assetUuid == null ? null
                : rbacAssetRepo.findVisibleByUuid(assetUuid).orElseThrow(
                        () -> new NoSuchElementException("cannot find HostingAsset by assignedToAsset.uuid: " + assetUuid));
    }
}
