package net.hostsharing.hsadminng.hs.office.relation;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeRelationPatchResource;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;

import jakarta.persistence.EntityManager;
import java.util.UUID;

class HsOfficeRelationEntityPatcher implements EntityPatcher<HsOfficeRelationPatchResource> {

    private final EntityManager em;
    private final HsOfficeRelationEntity entity;

    HsOfficeRelationEntityPatcher(final EntityManager em, final HsOfficeRelationEntity entity) {
        this.em = em;
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficeRelationPatchResource resource) {
        OptionalFromJson.of(resource.getContactUuid()).ifPresent(newValue -> {
            verifyNotNull(newValue, "contact");
            entity.setContact(em.getReference(HsOfficeContactEntity.class, newValue));
        });
    }

    private void verifyNotNull(final UUID newValue, final String propertyName) {
        if (newValue == null) {
            throw new IllegalArgumentException("property '" + propertyName + "' must not be null");
        }
    }
}
