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

        if (resource.getPartnerRel() != null) {
            new HsOfficeRelationPatcher(mapper, em, entity.getPartnerRel()).apply(resource.getPartnerRel());
        }

        if (resource.getDetails() != null) {
            new HsOfficePartnerDetailsEntityPatcher(em, entity.getDetails()).apply(resource.getDetails());
        }
    }
}
