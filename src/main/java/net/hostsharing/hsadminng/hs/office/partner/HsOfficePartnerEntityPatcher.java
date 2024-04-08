package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerPatchResource;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationEntity;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;

import jakarta.persistence.EntityManager;

class HsOfficePartnerEntityPatcher implements EntityPatcher<HsOfficePartnerPatchResource> {
    private final EntityManager em;
    private final HsOfficePartnerEntity entity;
    HsOfficePartnerEntityPatcher(
            final EntityManager em,
            final HsOfficePartnerEntity entity) {
        this.em = em;
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficePartnerPatchResource resource) {
        OptionalFromJson.of(resource.getPartnerRelUuid()).ifPresent(newValue -> {
            verifyNotNull(newValue, "partnerRel");
            entity.setPartnerRel(em.getReference(HsOfficeRelationEntity.class, newValue));
        });

        new HsOfficePartnerDetailsEntityPatcher(em, entity.getDetails()).apply(resource.getDetails());
    }

    private void verifyNotNull(final Object newValue, final String propertyName) {
        if (newValue == null) {
            throw new IllegalArgumentException("property '" + propertyName + "' must not be null");
        }
    }
}
