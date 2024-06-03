package net.hostsharing.hsadminng.hs.booking.project;

import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingProjectPatchResource;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;



public class HsBookingProjectEntityPatcher implements EntityPatcher<HsBookingProjectPatchResource> {

    private final HsBookingProjectEntity entity;

    public HsBookingProjectEntityPatcher(final HsBookingProjectEntity entity) {
        this.entity = entity;
    }

    @Override
    public void apply(final HsBookingProjectPatchResource resource) {
        OptionalFromJson.of(resource.getCaption())
                .ifPresent(entity::setCaption);
    }
}
