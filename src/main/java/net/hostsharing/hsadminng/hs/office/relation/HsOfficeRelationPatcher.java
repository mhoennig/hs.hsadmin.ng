package net.hostsharing.hsadminng.hs.office.relation;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRbacRepository;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeRelationPatchResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRbacRepository;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.StrictMapper;

import jakarta.persistence.EntityManager;
import jakarta.validation.ValidationException;
import java.util.NoSuchElementException;
import java.util.UUID;

public class HsOfficeRelationPatcher implements EntityPatcher<HsOfficeRelationPatchResource> {

    private final StrictMapper mapper;
    private final EntityManager em;
    private final HsOfficePersonRbacRepository rbacPersonRepo;
    private final HsOfficeContactRbacRepository rbacContactRepo;
    private final HsOfficeRelation entity;

    public HsOfficeRelationPatcher(
            final StrictMapper mapper,
            final EntityManager em,
            final HsOfficePersonRbacRepository rbacPersonRepo,
            final HsOfficeContactRbacRepository rbacContactRepo,
            final HsOfficeRelation entity) {
        this.mapper = mapper;
        this.em = em;
        this.rbacPersonRepo = rbacPersonRepo;
        this.rbacContactRepo = rbacContactRepo;
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
                entity.setHolder(visiblePersonOrNull(resource.getHolderUuid().get()));
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
                entity.setContact(visibleContactOrNull(resource.getContactUuid().get()));
            }
        }
    }

    private HsOfficePersonRealEntity visiblePersonOrNull(final UUID personUuid) {
        return personUuid == null ? null
                : rbacPersonRepo.findVisibleByUuid(personUuid).orElseThrow(
                        () -> new NoSuchElementException("cannot find Person by holder.uuid: " + personUuid));
    }

    private HsOfficeContactRealEntity visibleContactOrNull(final UUID contactUuid) {
        return contactUuid == null ? null
                : rbacContactRepo.findVisibleByUuid(contactUuid).orElseThrow(
                        () -> new NoSuchElementException("cannot find Contact by contact.uuid: " + contactUuid));
    }
}
