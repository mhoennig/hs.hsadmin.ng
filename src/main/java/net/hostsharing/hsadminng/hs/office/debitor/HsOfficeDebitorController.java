package net.hostsharing.hsadminng.hs.office.debitor;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeDebitorsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeDebitorInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeDebitorPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeDebitorResource;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRepository;
import net.hostsharing.hsadminng.mapper.Mapper;
import org.apache.commons.lang3.Validate;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType.DEBITOR;

@RestController

public class HsOfficeDebitorController implements HsOfficeDebitorsApi {

    @Autowired
    private Context context;

    @Autowired
    private Mapper mapper;

    @Autowired
    private HsOfficeDebitorRepository debitorRepo;

    @Autowired
    private HsOfficeRelationRepository relRepo;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficeDebitorResource>> listDebitors(
            final String currentUser,
            final String assumedRoles,
            final String name,
            final Integer debitorNumber) {
        context.define(currentUser, assumedRoles);

        final var entities = debitorNumber != null
                ? debitorRepo.findDebitorByDebitorNumber(debitorNumber)
                : debitorRepo.findDebitorByOptionalNameLike(name);

        final var resources = mapper.mapList(entities, HsOfficeDebitorResource.class);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeDebitorResource> addDebitor(
            String currentUser,
            String assumedRoles,
            HsOfficeDebitorInsertResource body) {

        context.define(currentUser, assumedRoles);

        Validate.isTrue(body.getDebitorRel() == null || body.getDebitorRelUuid() == null,
                "ERROR: [400] exactly one of debitorRel and debitorRelUuid must be supplied, but found both");
        Validate.isTrue(body.getDebitorRel() != null || body.getDebitorRelUuid() != null,
                "ERROR: [400] exactly one of debitorRel and debitorRelUuid must be supplied, but found none");
        Validate.isTrue(body.getDebitorRel() == null ||
                    body.getDebitorRel().getType() == null || DEBITOR.name().equals(body.getDebitorRel().getType()),
                "ERROR: [400] debitorRel.type must be '"+DEBITOR.name()+"' or null for default");
        Validate.isTrue(body.getDebitorRel() == null || body.getDebitorRel().getMark() == null,
                "ERROR: [400] debitorRel.mark must be null");

        final var entityToSave = mapper.map(body, HsOfficeDebitorEntity.class);
        if ( body.getDebitorRel() != null ) {
            body.getDebitorRel().setType(DEBITOR.name());
            final var debitorRel = mapper.map(body.getDebitorRel(), HsOfficeRelationEntity.class);
            entityToSave.setDebitorRel(relRepo.save(debitorRel));
        } else {
            final var debitorRelOptional = relRepo.findByUuid(body.getDebitorRelUuid());
            debitorRelOptional.ifPresentOrElse(
                    debitorRel -> {entityToSave.setDebitorRel(relRepo.save(debitorRel));},
                    () -> { throw new EntityNotFoundException("ERROR: [400] debitorRelUuid not found: " + body.getDebitorRelUuid());});
        }

        final var savedEntity = debitorRepo.save(entityToSave);
        em.flush();
        em.refresh(savedEntity);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/debitors/{id}")
                        .buildAndExpand(savedEntity.getUuid())
                        .toUri();
        final var mapped = mapper.map(savedEntity, HsOfficeDebitorResource.class);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<HsOfficeDebitorResource> getDebitorByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID debitorUuid) {

        context.define(currentUser, assumedRoles);

        final var result = debitorRepo.findByUuid(debitorUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result.get(), HsOfficeDebitorResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteDebitorByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID debitorUuid) {
        context.define(currentUser, assumedRoles);

        final var result = debitorRepo.deleteByUuid(debitorUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeDebitorResource> patchDebitor(
            final String currentUser,
            final String assumedRoles,
            final UUID debitorUuid,
            final HsOfficeDebitorPatchResource body) {

        context.define(currentUser, assumedRoles);

        final var current = debitorRepo.findByUuid(debitorUuid).orElseThrow();

        new HsOfficeDebitorEntityPatcher(em, current).apply(body);

        final var saved = debitorRepo.save(current);
        Hibernate.initialize(saved);
        final var mapped = mapper.map(saved, HsOfficeDebitorResource.class);
        return ResponseEntity.ok(mapped);
    }
}
