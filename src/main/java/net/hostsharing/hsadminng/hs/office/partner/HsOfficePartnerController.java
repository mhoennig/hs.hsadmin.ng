package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.errors.ReferenceNotFoundException;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficePartnersApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerRelInsertResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealRepository;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import net.hostsharing.hsadminng.persistence.BaseEntity;
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

import static net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType.EX_PARTNER;
import static net.hostsharing.hsadminng.repr.TaggedNumber.cropTag;

@RestController

public class HsOfficePartnerController implements HsOfficePartnersApi {

    @Autowired
    private Context context;

    @Autowired
    private StandardMapper mapper;

    @Autowired
    private HsOfficePartnerRepository partnerRepo;

    @Autowired
    private HsOfficeRelationRealRepository relationRepo;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficePartnerResource>> listPartners(
            final String currentSubject,
            final String assumedRoles,
            final String name) {
        context.define(currentSubject, assumedRoles);

        final var entities = partnerRepo.findPartnerByOptionalNameLike(name);

        final var resources = mapper.mapList(entities, HsOfficePartnerResource.class);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficePartnerResource> addPartner(
            final String currentSubject,
            final String assumedRoles,
            final HsOfficePartnerInsertResource body) {

        context.define(currentSubject, assumedRoles);

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
            final String currentSubject,
            final String assumedRoles,
            final UUID partnerUuid) {

        context.define(currentSubject, assumedRoles);

        final var result = partnerRepo.findByUuid(partnerUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result.get(), HsOfficePartnerResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deletePartnerByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID partnerUuid) {
        context.define(currentSubject, assumedRoles);

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
            final String currentSubject,
            final String assumedRoles,
            final UUID partnerUuid,
            final HsOfficePartnerPatchResource body) {

        context.define(currentSubject, assumedRoles);

        final var current = partnerRepo.findByUuid(partnerUuid).orElseThrow();
        final var previousPartnerRel = current.getPartnerRel();

        new HsOfficePartnerEntityPatcher(em, current).apply(body);

        final var saved = partnerRepo.save(current);
        optionallyCreateExPartnerRelation(saved, previousPartnerRel);

        final var mapped = mapper.map(saved, HsOfficePartnerResource.class);
        return ResponseEntity.ok(mapped);
    }

    private void optionallyCreateExPartnerRelation(final HsOfficePartnerEntity saved, final HsOfficeRelationRealEntity previousPartnerRel) {
        if (!saved.getPartnerRel().getUuid().equals(previousPartnerRel.getUuid())) {
            // TODO.impl: we also need to use the new partner-person as the anchor
            relationRepo.save(previousPartnerRel.toBuilder().uuid(null).type(EX_PARTNER).build());
        }
    }

    private HsOfficePartnerEntity createPartnerEntity(final HsOfficePartnerInsertResource body) {
        final var entityToSave = new HsOfficePartnerEntity();
        entityToSave.setPartnerNumber(cropTag(HsOfficePartnerEntity.PARTNER_NUMBER_TAG, body.getPartnerNumber()));
        entityToSave.setPartnerRel(persistPartnerRel(body.getPartnerRel()));
        entityToSave.setDetails(mapper.map(body.getDetails(), HsOfficePartnerDetailsEntity.class));
        return entityToSave;
    }

    private HsOfficeRelationRealEntity persistPartnerRel(final HsOfficePartnerRelInsertResource resource) {
        final var entity = new HsOfficeRelationRealEntity();
        entity.setType(HsOfficeRelationType.PARTNER);
        entity.setAnchor(ref(HsOfficePersonEntity.class, resource.getAnchorUuid()));
        entity.setHolder(ref(HsOfficePersonEntity.class, resource.getHolderUuid()));
        entity.setContact(ref(HsOfficeContactRealEntity.class, resource.getContactUuid()));
        em.persist(entity);
        return entity;
    }

    private <E extends BaseEntity> E ref(final Class<E> entityClass, final UUID uuid) {
        try {
            return em.getReference(entityClass, uuid);
        } catch (final Throwable exc) {
                throw new ReferenceNotFoundException(entityClass, uuid, exc);
        }
    }
}
