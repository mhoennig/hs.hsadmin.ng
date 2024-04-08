package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.errors.ReferenceNotFoundException;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficePartnersApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerRelInsertResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRepository;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType;
import net.hostsharing.hsadminng.mapper.Mapper;
import net.hostsharing.hsadminng.rbac.rbacobject.RbacObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

@RestController

public class HsOfficePartnerController implements HsOfficePartnersApi {

    @Autowired
    private Context context;

    @Autowired
    private Mapper mapper;

    @Autowired
    private HsOfficePartnerRepository partnerRepo;

    @Autowired
    private HsOfficeRelationRepository relationRepo;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficePartnerResource>> listPartners(
            final String currentUser,
            final String assumedRoles,
            final String name) {
        context.define(currentUser, assumedRoles);

        final var entities = partnerRepo.findPartnerByOptionalNameLike(name);

        final var resources = mapper.mapList(entities, HsOfficePartnerResource.class);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficePartnerResource> addPartner(
            final String currentUser,
            final String assumedRoles,
            final HsOfficePartnerInsertResource body) {

        context.define(currentUser, assumedRoles);

        final var entityToSave = createPartnerEntity(body);

        final var saved = partnerRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/partners/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsOfficePartnerResource.class);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<HsOfficePartnerResource> getPartnerByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID partnerUuid) {

        context.define(currentUser, assumedRoles);

        final var result = partnerRepo.findByUuid(partnerUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result.get(), HsOfficePartnerResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deletePartnerByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID partnerUuid) {
        context.define(currentUser, assumedRoles);

        final var partnerToDelete = partnerRepo.findByUuid(partnerUuid);
        if (partnerToDelete.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (partnerRepo.deleteByUuid(partnerUuid) != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficePartnerResource> patchPartner(
            final String currentUser,
            final String assumedRoles,
            final UUID partnerUuid,
            final HsOfficePartnerPatchResource body) {

        context.define(currentUser, assumedRoles);

        final var current = partnerRepo.findByUuid(partnerUuid).orElseThrow();

        new HsOfficePartnerEntityPatcher(em, current).apply(body);

        final var saved = partnerRepo.save(current);
        final var mapped = mapper.map(saved, HsOfficePartnerResource.class);
        return ResponseEntity.ok(mapped);
    }

    private HsOfficePartnerEntity createPartnerEntity(final HsOfficePartnerInsertResource body) {
        final var entityToSave = new HsOfficePartnerEntity();
        entityToSave.setPartnerNumber(body.getPartnerNumber());
        entityToSave.setPartnerRel(persistPartnerRel(body.getPartnerRel()));
        entityToSave.setDetails(mapper.map(body.getDetails(), HsOfficePartnerDetailsEntity.class));
        return entityToSave;
    }

    private HsOfficeRelationEntity persistPartnerRel(final HsOfficePartnerRelInsertResource resource) {
        final var entity = new HsOfficeRelationEntity();
        entity.setType(HsOfficeRelationType.PARTNER);
        entity.setAnchor(ref(HsOfficePersonEntity.class, resource.getAnchorUuid()));
        entity.setHolder(ref(HsOfficePersonEntity.class, resource.getHolderUuid()));
        entity.setContact(ref(HsOfficeContactEntity.class, resource.getContactUuid()));
        em.persist(entity);
        return entity;
    }

    private <E extends RbacObject> E ref(final Class<E> entityClass, final UUID uuid) {
        try {
            return em.getReference(entityClass, uuid);
        } catch (final Throwable exc) {
                throw new ReferenceNotFoundException(entityClass, uuid, exc);
        }
    }
}
