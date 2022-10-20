package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.EntityPatcher;
import net.hostsharing.hsadminng.OptionalFromJson;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerDetailsPatchResource;

import javax.persistence.EntityManager;
import java.util.UUID;

class HsOfficePartnerDetailsEntityPatcher implements EntityPatcher<HsOfficePartnerDetailsPatchResource> {

    private final EntityManager em;
    private final HsOfficePartnerDetailsEntity entity;

    HsOfficePartnerDetailsEntityPatcher(
            final EntityManager em,
            final HsOfficePartnerDetailsEntity entity) {
        this.em = em;
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficePartnerDetailsPatchResource resource) {
        if (resource != null) {
            OptionalFromJson.of(resource.getRegistrationOffice()).ifPresent(entity::setRegistrationOffice);
            OptionalFromJson.of(resource.getRegistrationNumber()).ifPresent(entity::setRegistrationNumber);
            OptionalFromJson.of(resource.getBirthday()).ifPresent(entity::setBirthday);
            OptionalFromJson.of(resource.getBirthName()).ifPresent(entity::setBirthName);
            OptionalFromJson.of(resource.getDateOfDeath()).ifPresent(entity::setDateOfDeath);
        }
    }
}
