package net.hostsharing.hsadminng.hs.office.contact;

import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeContactPatchResource;

class HsOfficeContactEntityPatch implements EntityPatcher<HsOfficeContactPatchResource> {

    private final HsOfficeContactEntity entity;

    HsOfficeContactEntityPatch(final HsOfficeContactEntity entity) {
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficeContactPatchResource resource) {
        OptionalFromJson.of(resource.getLabel()).ifPresent(entity::setLabel);
        OptionalFromJson.of(resource.getPostalAddress()).ifPresent(entity::setPostalAddress);
        OptionalFromJson.of(resource.getEmailAddresses()).ifPresent(entity::setEmailAddresses);
        OptionalFromJson.of(resource.getPhoneNumbers()).ifPresent(entity::setPhoneNumbers);
    }
}
