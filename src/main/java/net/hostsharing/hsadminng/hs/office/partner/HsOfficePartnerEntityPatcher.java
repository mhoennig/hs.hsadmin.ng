package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerPatchResource;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationPatcher;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.StrictMapper;

import jakarta.persistence.EntityManager;

class HsOfficePartnerEntityPatcher implements EntityPatcher<HsOfficePartnerPatchResource> {

    private final StrictMapper mapper;
    private final EntityManager em;
    private final HsOfficePartnerRbacEntity entity;

    HsOfficePartnerEntityPatcher(
            final StrictMapper mapper,
            final EntityManager em,
            final HsOfficePartnerRbacEntity entity) {
        this.mapper = mapper;
        this.em = em;
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficePartnerPatchResource resource) {

        // HOWTO: allow properties from the GET request to be passed to PATCH, but only if unchanged.
        //  These properties have to be specified in the OpenAPI PATCH resource specification,
        //  ídeally with a comment in the description field, that the value mist be unchanged, if given at all.
        ignoreUnchangedPropertyValue("uuid", resource.getUuid(), entity.getUuid());
        ignoreUnchangedPropertyValue("partnerNumber", resource.getPartnerNumber(), entity.getPartnerNumber().toString());

        if (resource.getPartnerRel() != null) {
            new HsOfficeRelationPatcher(mapper, em, entity.getPartnerRel()).apply(resource.getPartnerRel());
        }

        if (resource.getDetails() != null) {
            new HsOfficePartnerDetailsEntityPatcher(em, entity.getDetails()).apply(resource.getDetails());
        }
    }
}
