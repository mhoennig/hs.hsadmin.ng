package net.hostsharing.hsadminng.hs.office.relationship;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeRelationshipsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.*;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRepository;
import net.hostsharing.hsadminng.mapper.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.BiConsumer;


@RestController

public class HsOfficeRelationshipController implements HsOfficeRelationshipsApi {

    @Autowired
    private Context context;

    @Autowired
    private Mapper mapper;

    @Autowired
    private HsOfficeRelationshipRepository relationshipRepo;

    @Autowired
    private HsOfficePersonRepository relHolderRepo;

    @Autowired
    private HsOfficeContactRepository contactRepo;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficeRelationshipResource>> listRelationships(
            final String currentUser,
            final String assumedRoles,
            final UUID personUuid,
            final HsOfficeRelationshipTypeResource relationshipType) {
        context.define(currentUser, assumedRoles);

        final var entities = relationshipRepo.findRelationshipRelatedToPersonUuid(personUuid,
                mapper.map(relationshipType, HsOfficeRelationshipType.class));

        final var resources = mapper.mapList(entities, HsOfficeRelationshipResource.class,
                RELATIONSHIP_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeRelationshipResource> addRelationship(
            final String currentUser,
            final String assumedRoles,
            final HsOfficeRelationshipInsertResource body) {

        context.define(currentUser, assumedRoles);

        final var entityToSave = new HsOfficeRelationshipEntity();
        entityToSave.setRelType(HsOfficeRelationshipType.valueOf(body.getRelType()));
        entityToSave.setRelAnchor(relHolderRepo.findByUuid(body.getRelAnchorUuid()).orElseThrow(
                () -> new NoSuchElementException("cannot find relAnchorUuid " + body.getRelAnchorUuid())
        ));
        entityToSave.setRelHolder(relHolderRepo.findByUuid(body.getRelHolderUuid()).orElseThrow(
                () -> new NoSuchElementException("cannot find relHolderUuid " + body.getRelHolderUuid())
        ));
        entityToSave.setContact(contactRepo.findByUuid(body.getContactUuid()).orElseThrow(
                () -> new NoSuchElementException("cannot find contactUuid " + body.getContactUuid())
        ));

        final var saved = relationshipRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/relationships/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsOfficeRelationshipResource.class,
                RELATIONSHIP_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<HsOfficeRelationshipResource> getRelationshipByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID relationshipUuid) {

        context.define(currentUser, assumedRoles);

        final var result = relationshipRepo.findByUuid(relationshipUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result.get(), HsOfficeRelationshipResource.class, RELATIONSHIP_ENTITY_TO_RESOURCE_POSTMAPPER));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteRelationshipByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID relationshipUuid) {
        context.define(currentUser, assumedRoles);

        final var result = relationshipRepo.deleteByUuid(relationshipUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeRelationshipResource> patchRelationship(
            final String currentUser,
            final String assumedRoles,
            final UUID relationshipUuid,
            final HsOfficeRelationshipPatchResource body) {

        context.define(currentUser, assumedRoles);

        final var current = relationshipRepo.findByUuid(relationshipUuid).orElseThrow();

        new HsOfficeRelationshipEntityPatcher(em, current).apply(body);

        final var saved = relationshipRepo.save(current);
        final var mapped = mapper.map(saved, HsOfficeRelationshipResource.class);
        return ResponseEntity.ok(mapped);
    }


    final BiConsumer<HsOfficeRelationshipEntity, HsOfficeRelationshipResource> RELATIONSHIP_ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setRelAnchor(mapper.map(entity.getRelAnchor(), HsOfficePersonResource.class));
        resource.setRelHolder(mapper.map(entity.getRelHolder(), HsOfficePersonResource.class));
        resource.setContact(mapper.map(entity.getContact(), HsOfficeContactResource.class));
    };
}
