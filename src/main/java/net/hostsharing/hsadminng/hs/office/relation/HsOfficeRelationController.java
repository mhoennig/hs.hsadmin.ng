package net.hostsharing.hsadminng.hs.office.relation;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.errors.Validate;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealRepository;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeRelationsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.*;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.BiConsumer;

import static net.hostsharing.hsadminng.mapper.KeyValueMap.from;

@RestController
public class HsOfficeRelationController implements HsOfficeRelationsApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private HsOfficeRelationRbacRepository rbacRelationRepo;

    @Autowired
    private HsOfficePersonRealRepository realPersonRepo;

    @Autowired
    private HsOfficeContactRealRepository realContactRepo;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.relations.api.getListOfRelations")
    public ResponseEntity<List<HsOfficeRelationResource>> getListOfRelations(
            final String currentSubject,
            final String assumedRoles,
            final UUID personUuid,
            final HsOfficeRelationTypeResource relationType,
            final String mark,
            final String personData,
            final String contactData) {
        context.define(currentSubject, assumedRoles);

        final List<HsOfficeRelationRbacEntity> entities =
                rbacRelationRepo.findRelationRelatedToPersonUuidRelationTypeMarkPersonAndContactData(
                        personUuid,
                        relationType == null ? null : HsOfficeRelationType.valueOf(relationType.name()),
                        mark, personData, contactData);

        final var resources = mapper.mapList(entities, HsOfficeRelationResource.class,
                RELATION_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    @Timed("app.office.relations.api.postNewRelation")
    public ResponseEntity<HsOfficeRelationResource> postNewRelation(
            final String currentSubject,
            final String assumedRoles,
            final HsOfficeRelationInsertResource body) {

        context.define(currentSubject, assumedRoles);

        final var entityToSave = new HsOfficeRelationRbacEntity();
        entityToSave.setType(HsOfficeRelationType.valueOf(body.getType()));
        entityToSave.setMark(body.getMark());

        entityToSave.setAnchor(realPersonRepo.findByUuid(body.getAnchorUuid()).orElseThrow(
                () -> new NoSuchElementException("cannot find Person by anchorUuid: " + body.getAnchorUuid())
        ));

        Validate.validate("anchor, anchor.uuid").exactlyOne(body.getHolder(), body.getHolderUuid());
        if ( body.getHolderUuid() != null) {
            entityToSave.setHolder(realPersonRepo.findByUuid(body.getHolderUuid()).orElseThrow(
                    () -> new NoSuchElementException("cannot find Person by holderUuid: " + body.getHolderUuid())
            ));
        } else {
            entityToSave.setHolder(realPersonRepo.save(
                    mapper.map(body.getHolder(), HsOfficePersonRealEntity.class)
            ) );
        }

        Validate.validate("contact, contact.uuid").exactlyOne(body.getContact(), body.getContactUuid());
        if ( body.getContactUuid() != null) {
            entityToSave.setContact(realContactRepo.findByUuid(body.getContactUuid()).orElseThrow(
                    () -> new NoSuchElementException("cannot find Contact by contactUuid: " + body.getContactUuid())
            ));
        } else {
            entityToSave.setContact(realContactRepo.save(
                    mapper.map(body.getContact(), HsOfficeContactRealEntity.class, CONTACT_RESOURCE_TO_ENTITY_POSTMAPPER)
            ) );
        }

        final var saved = rbacRelationRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/relations/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsOfficeRelationResource.class,
                RELATION_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.relations.api.getSingleRelationByUuid")
    public ResponseEntity<HsOfficeRelationResource> getSingleRelationByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID relationUuid) {

        context.define(currentSubject, assumedRoles);

        final var result = rbacRelationRepo.findByUuid(relationUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result.get(), HsOfficeRelationResource.class, RELATION_ENTITY_TO_RESOURCE_POSTMAPPER));
    }

    @Override
    @Transactional
    @Timed("apprelations.api..deleteRelationByUuid")
    public ResponseEntity<Void> deleteRelationByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID relationUuid) {
        context.define(currentSubject, assumedRoles);

        final var result = rbacRelationRepo.deleteByUuid(relationUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    @Timed("app.office.relations.api.patchRelation")
    public ResponseEntity<HsOfficeRelationResource> patchRelation(
            final String currentSubject,
            final String assumedRoles,
            final UUID relationUuid,
            final HsOfficeRelationPatchResource body) {

        context.define(currentSubject, assumedRoles);

        final var current = rbacRelationRepo.findByUuid(relationUuid).orElseThrow();

        new HsOfficeRelationEntityPatcher(em, current).apply(body);

        final var saved = rbacRelationRepo.save(current);
        final var mapped = mapper.map(saved, HsOfficeRelationResource.class);
        return ResponseEntity.ok(mapped);
    }

    final BiConsumer<HsOfficeRelationRbacEntity, HsOfficeRelationResource> RELATION_ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setAnchor(mapper.map(entity.getAnchor(), HsOfficePersonResource.class));
        resource.setHolder(mapper.map(entity.getHolder(), HsOfficePersonResource.class));
        resource.setContact(mapper.map(entity.getContact(), HsOfficeContactResource.class));
    };


    @SuppressWarnings("unchecked")
    final BiConsumer<HsOfficeContactInsertResource, HsOfficeContactRealEntity> CONTACT_RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        entity.putEmailAddresses(from(resource.getEmailAddresses()));
        entity.putPhoneNumbers(from(resource.getPhoneNumbers()));
    };
}
