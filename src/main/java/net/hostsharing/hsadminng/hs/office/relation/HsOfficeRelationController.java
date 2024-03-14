package net.hostsharing.hsadminng.hs.office.relation;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeRelationsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.*;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRepository;
import net.hostsharing.hsadminng.mapper.Mapper;
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


@RestController

public class HsOfficeRelationController implements HsOfficeRelationsApi {

    @Autowired
    private Context context;

    @Autowired
    private Mapper mapper;

    @Autowired
    private HsOfficeRelationRepository relationRepo;

    @Autowired
    private HsOfficePersonRepository holderRepo;

    @Autowired
    private HsOfficeContactRepository contactRepo;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficeRelationResource>> listRelations(
            final String currentUser,
            final String assumedRoles,
            final UUID personUuid,
            final HsOfficeRelationTypeResource relationType) {
        context.define(currentUser, assumedRoles);

        final var entities = relationRepo.findRelationRelatedToPersonUuidAndRelationType(personUuid,
                mapper.map(relationType, HsOfficeRelationType.class));

        final var resources = mapper.mapList(entities, HsOfficeRelationResource.class,
                RELATION_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeRelationResource> addRelation(
            final String currentUser,
            final String assumedRoles,
            final HsOfficeRelationInsertResource body) {

        context.define(currentUser, assumedRoles);

        final var entityToSave = new HsOfficeRelationEntity();
        entityToSave.setType(HsOfficeRelationType.valueOf(body.getType()));
        entityToSave.setMark(body.getMark());
        entityToSave.setAnchor(holderRepo.findByUuid(body.getAnchorUuid()).orElseThrow(
                () -> new NoSuchElementException("cannot find anchorUuid " + body.getAnchorUuid())
        ));
        entityToSave.setHolder(holderRepo.findByUuid(body.getHolderUuid()).orElseThrow(
                () -> new NoSuchElementException("cannot find holderUuid " + body.getHolderUuid())
        ));
        entityToSave.setContact(contactRepo.findByUuid(body.getContactUuid()).orElseThrow(
                () -> new NoSuchElementException("cannot find contactUuid " + body.getContactUuid())
        ));

        final var saved = relationRepo.save(entityToSave);

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
    public ResponseEntity<HsOfficeRelationResource> getRelationByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID relationUuid) {

        context.define(currentUser, assumedRoles);

        final var result = relationRepo.findByUuid(relationUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result.get(), HsOfficeRelationResource.class, RELATION_ENTITY_TO_RESOURCE_POSTMAPPER));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteRelationByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID relationUuid) {
        context.define(currentUser, assumedRoles);

        final var result = relationRepo.deleteByUuid(relationUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeRelationResource> patchRelation(
            final String currentUser,
            final String assumedRoles,
            final UUID relationUuid,
            final HsOfficeRelationPatchResource body) {

        context.define(currentUser, assumedRoles);

        final var current = relationRepo.findByUuid(relationUuid).orElseThrow();

        new HsOfficeRelationEntityPatcher(em, current).apply(body);

        final var saved = relationRepo.save(current);
        final var mapped = mapper.map(saved, HsOfficeRelationResource.class);
        return ResponseEntity.ok(mapped);
    }


    final BiConsumer<HsOfficeRelationEntity, HsOfficeRelationResource> RELATION_ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setAnchor(mapper.map(entity.getAnchor(), HsOfficePersonResource.class));
        resource.setHolder(mapper.map(entity.getHolder(), HsOfficePersonResource.class));
        resource.setContact(mapper.map(entity.getContact(), HsOfficeContactResource.class));
    };
}
