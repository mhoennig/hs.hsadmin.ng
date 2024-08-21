package net.hostsharing.hsadminng.hs.booking.project;

import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingProjectPatchResource;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;



public class HsBookingProjectEntityPatcher implements EntityPatcher<HsBookingProjectPatchResource> {

    private final HsBookingProject entity;

    public HsBookingProjectEntityPatcher(final HsBookingProject entity) {
        this.entity = entity;
    }

    @Override
    public void apply(final HsBookingProjectPatchResource resource) {
        OptionalFromJson.of(resource.getCaption())
                .ifPresent(entity::setCaption);
    }
}
