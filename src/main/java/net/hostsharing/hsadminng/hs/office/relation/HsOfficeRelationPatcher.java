package net.hostsharing.hsadminng.hs.office.relation;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeRelationPatchResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.StrictMapper;

import jakarta.persistence.EntityManager;
import jakarta.validation.ValidationException;

public class HsOfficeRelationPatcher implements EntityPatcher<HsOfficeRelationPatchResource> {

    private final StrictMapper mapper;
    private final EntityManager em;
    private final HsOfficeRelation entity;

    public HsOfficeRelationPatcher(final StrictMapper mapper, final EntityManager em, final HsOfficeRelation entity) {
        this.mapper = mapper;
        this.em = em;
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficeRelationPatchResource resource) {
        if (resource.getHolder() != null && resource.getHolderUuid() != null) {
            throw new ValidationException("either \"holder\" or \"holder.uuid\" can be given, not both");
        } else {
            if (resource.getHolder() != null) {
                final var newHolder = mapper.map(resource.getHolder(), HsOfficePersonRealEntity.class);
                em.persist(newHolder);
                entity.setHolder(newHolder);
            } else if (resource.getHolderUuid() != null) {
                entity.setHolder(em.getReference(HsOfficePersonRealEntity.class, resource.getHolderUuid().get()));
            }
        }

        if (resource.getContact() != null && resource.getContactUuid() != null) {
            throw new ValidationException("either \"contact\" or \"contact.uuid\" can be given, not both");
        } else {
            if (resource.getContact() != null) {
                final var newContact = mapper.map(resource.getContact(), HsOfficeContactRealEntity.class);
                em.persist(newContact);
                entity.setContact(newContact);
            } else if (resource.getContactUuid() != null) {
                entity.setContact(em.getReference(HsOfficeContactRealEntity.class, resource.getContactUuid().get()));
            }
        }
    }
}
