package net.hostsharing.hsadminng.hs.office.contact;

import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.KeyValueMap;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeContactPatchResource;

import java.util.Optional;

class HsOfficeContactEntityPatcher implements EntityPatcher<HsOfficeContactPatchResource> {

    private final HsOfficeContactRbacEntity entity;

    HsOfficeContactEntityPatcher(final HsOfficeContactRbacEntity entity) {
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficeContactPatchResource resource) {
        OptionalFromJson.of(resource.getCaption()).ifPresent(entity::setCaption);
        OptionalFromJson.of(resource.getPostalAddress()).ifPresent(entity::setPostalAddress);
        Optional.ofNullable(resource.getEmailAddresses())
                .ifPresent(r -> entity.getEmailAddresses().patch(KeyValueMap.from(resource.getEmailAddresses())));
        Optional.ofNullable(resource.getPhoneNumbers())
                .ifPresent(r -> entity.getPhoneNumbers().patch(KeyValueMap.from(resource.getPhoneNumbers())));
    }
}
