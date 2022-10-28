package net.hostsharing.hsadminng.hs.office.debitor;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeDebitorsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeDebitorInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeDebitorPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeDebitorResource;
import net.hostsharing.hsadminng.mapper.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

@RestController

public class HsOfficeDebitorController implements HsOfficeDebitorsApi {

    @Autowired
    private Context context;

    @Autowired
    private Mapper mapper;

    @Autowired
    private HsOfficeDebitorRepository debitorRepo;

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
            final String currentUser,
            final String assumedRoles,
            final HsOfficeDebitorInsertResource body) {

        context.define(currentUser, assumedRoles);

        final var entityToSave = mapper.map(body, HsOfficeDebitorEntity.class);

        final var saved = debitorRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/debitors/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsOfficeDebitorResource.class);
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
        final var mapped = mapper.map(saved, HsOfficeDebitorResource.class);
        return ResponseEntity.ok(mapped);
    }
}
