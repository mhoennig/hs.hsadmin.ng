package net.hostsharing.hsadminng.hs.booking.item;

import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingItemPatchResource;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.KeyValueMap;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;

import java.util.Optional;


public class HsBookingItemEntityPatcher implements EntityPatcher<HsBookingItemPatchResource> {

    private final HsBookingItemEntity entity;

    public HsBookingItemEntityPatcher(final HsBookingItemEntity entity) {
        this.entity = entity;
    }

    @Override
    public void apply(final HsBookingItemPatchResource resource) {
        OptionalFromJson.of(resource.getCaption())
                .ifPresent(entity::setCaption);
        Optional.ofNullable(resource.getResources())
                .ifPresent(r -> entity.getResources().patch(KeyValueMap.from(resource.getResources())));
        OptionalFromJson.of(resource.getValidTo())
                .ifPresent(entity::setValidTo);
    }
}
