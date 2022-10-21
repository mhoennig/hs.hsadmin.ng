package net.hostsharing.hsadminng.hs.office.relationship;

import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeRelationshipPatchResource;

import javax.persistence.EntityManager;
import java.util.UUID;

class HsOfficeRelationshipEntityPatcher implements EntityPatcher<HsOfficeRelationshipPatchResource> {

    private final EntityManager em;
    private final HsOfficeRelationshipEntity entity;

    HsOfficeRelationshipEntityPatcher(final EntityManager em, final HsOfficeRelationshipEntity entity) {
        this.em = em;
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficeRelationshipPatchResource resource) {
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
