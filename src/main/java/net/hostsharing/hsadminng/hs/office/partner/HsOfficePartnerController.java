package net.hostsharing.hsadminng.hs.office.partner;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.errors.ReferenceNotFoundException;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactFromResourceConverter;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficePartnersApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeContactInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerRelInsertResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealRepository;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import static net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType.EX_PARTNER;
import static net.hostsharing.hsadminng.repr.TaggedNumber.cropTag;

@RestController
@SecurityRequirement(name = "casTicket")
public class HsOfficePartnerController implements HsOfficePartnersApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private HsOfficeContactFromResourceConverter<HsOfficeContactRealEntity> contactFromResourceConverter;

    @Autowired
    private HsOfficePartnerRbacRepository rbacPartnerRepo;

    @Autowired
    private HsOfficeRelationRealRepository realRelationRepo;

    @PersistenceContext
    private EntityManager em;

    @PostConstruct
    public void init() {
        mapper.addConverter(contactFromResourceConverter, HsOfficeContactInsertResource.class, HsOfficeContactRealEntity.class);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.partners.api.getListOfPartners")
    public ResponseEntity<List<HsOfficePartnerResource>> getListOfPartners(
            final String currentSubject,
            final String assumedRoles,
            final String name) {
        context.define(currentSubject, assumedRoles);

        final var entities = rbacPartnerRepo.findPartnerByOptionalNameLike(name);

        final var resources = mapper.mapList(entities, HsOfficePartnerResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    @Timed("app.office.partners.api.postNewPartner")
    public ResponseEntity<HsOfficePartnerResource> postNewPartner(
            final String currentSubject,
            final String assumedRoles,
            final HsOfficePartnerInsertResource body) {

        context.define(currentSubject, assumedRoles);

        final var entityToSave = createPartnerEntity(body);

        final var saved = rbacPartnerRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/partners/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsOfficePartnerResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.partners.api.getSinglePartnerByUuid")
    public ResponseEntity<HsOfficePartnerResource> getSinglePartnerByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID partnerUuid) {

        context.define(currentSubject, assumedRoles);

        final var result = rbacPartnerRepo.findByUuid(partnerUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        final var mapped = mapper.map(result.get(), HsOfficePartnerResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.partners.api.getSinglePartnerByPartnerNumber")
    public ResponseEntity<HsOfficePartnerResource> getSinglePartnerByPartnerNumber(
            final String currentSubject,
            final String assumedRoles,
            final Integer partnerNumber) {

        context.define(currentSubject, assumedRoles);

        final var result = rbacPartnerRepo.findPartnerByPartnerNumber(partnerNumber);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        final var mapped = mapper.map(result.get(), HsOfficePartnerResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(mapped);
    }

    @Override
    @Transactional
    @Timed("app.office.partners.api.deletePartnerByUuid")
    public ResponseEntity<Void> deletePartnerByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID partnerUuid) {
        context.define(currentSubject, assumedRoles);

        final var partnerToDelete = rbacPartnerRepo.findByUuid(partnerUuid);
        if (partnerToDelete.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (rbacPartnerRepo.deleteByUuid(partnerUuid) != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    @Timed("app.office.partners.api.patchPartner")
    public ResponseEntity<HsOfficePartnerResource> patchPartner(
            final String currentSubject,
            final String assumedRoles,
            final UUID partnerUuid,
            final HsOfficePartnerPatchResource body) {

        context.define(currentSubject, assumedRoles);

        final var current = rbacPartnerRepo.findByUuid(partnerUuid).orElseThrow();
        final var previousPartnerPerson = current.getPartnerRel().getHolder();

        new HsOfficePartnerEntityPatcher(mapper, em, current).apply(body);

        final var saved = rbacPartnerRepo.save(current);
        optionallyCreateExPartnerRelation(saved, previousPartnerPerson);
        optionallyUpdateRelatedRelations(saved, previousPartnerPerson);

        final var mapped = mapper.map(saved, HsOfficePartnerResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(mapped);
    }

    private void optionallyCreateExPartnerRelation(final HsOfficePartnerRbacEntity saved, final HsOfficePersonRealEntity previousPartnerPerson) {

        final var partnerPersonHasChanged = !saved.getPartnerRel().getHolder().getUuid().equals(previousPartnerPerson.getUuid());
        if (partnerPersonHasChanged) {
            realRelationRepo.save(saved.getPartnerRel().toBuilder()
                    .uuid(null)
                    .type(EX_PARTNER)
                    .anchor(saved.getPartnerRel().getHolder())
                    .holder(previousPartnerPerson)
                    .build());
        }
    }

    private void optionallyUpdateRelatedRelations(final HsOfficePartnerRbacEntity saved, final HsOfficePersonRealEntity previousPartnerPerson) {
        final var partnerPersonHasChanged = !saved.getPartnerRel().getHolder().getUuid().equals(previousPartnerPerson.getUuid());
        if (partnerPersonHasChanged) {
            // self-debitors of the old partner-person become self-debitors of the new partner person
            em.createNativeQuery("""
                UPDATE hs_office.relation
                    SET holderUuid = :newPartnerPersonUuid
                    WHERE type = 'DEBITOR' AND
                          holderUuid = :oldPartnerPersonUuid AND anchorUuid = :oldPartnerPersonUuid
            """)
                    .setParameter("oldPartnerPersonUuid", previousPartnerPerson.getUuid())
                    .setParameter("newPartnerPersonUuid", saved.getPartnerRel().getHolder().getUuid())
                    .executeUpdate();

            // re-anchor all relations from the old partner person to the new partner persion
            em.createNativeQuery("""
                UPDATE hs_office.relation
                    SET anchorUuid = :newPartnerPersonUuid
                    WHERE anchorUuid = :oldPartnerPersonUuid
            """)
                    .setParameter("oldPartnerPersonUuid", previousPartnerPerson.getUuid())
                    .setParameter("newPartnerPersonUuid", saved.getPartnerRel().getHolder().getUuid())
                    .executeUpdate();
        }
    }

    private HsOfficePartnerRbacEntity createPartnerEntity(final HsOfficePartnerInsertResource body) {
        final var entityToSave = new HsOfficePartnerRbacEntity();
        entityToSave.setPartnerNumber(cropTag(HsOfficePartnerRbacEntity.PARTNER_NUMBER_TAG, body.getPartnerNumber()));
        entityToSave.setPartnerRel(persistPartnerRel(body.getPartnerRel()));
        entityToSave.setDetails(mapper.map(body.getDetails(), HsOfficePartnerDetailsEntity.class));
        return entityToSave;
    }

    private HsOfficeRelationRealEntity persistPartnerRel(final HsOfficePartnerRelInsertResource resource) {
        final var entity = new HsOfficeRelationRealEntity();
        entity.setType(HsOfficeRelationType.PARTNER);
        entity.setAnchor(ref(HsOfficePersonRealEntity.class, resource.getAnchorUuid()));
        entity.setHolder(ref(HsOfficePersonRealEntity.class, resource.getHolderUuid()));
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

    final BiConsumer<HsOfficePartnerRbacEntity, HsOfficePartnerResource> ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setPartnerNumber(entity.getTaggedPartnerNumber());
    };
}
