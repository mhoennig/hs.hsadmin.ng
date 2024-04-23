package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetPatchResource;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.KeyValueMap;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;

import java.util.Optional;

public class HsHostingAssetEntityPatcher implements EntityPatcher<HsHostingAssetPatchResource> {

    private final HsHostingAssetEntity entity;

    public HsHostingAssetEntityPatcher(final HsHostingAssetEntity entity) {
        this.entity = entity;
    }

    @Override
    public void apply(final HsHostingAssetPatchResource resource) {
        OptionalFromJson.of(resource.getCaption())
                .ifPresent(entity::setCaption);
        Optional.ofNullable(resource.getConfig())
                .ifPresent(r -> entity.getConfig().patch(KeyValueMap.from(resource.getConfig())));
    }
}
