package net.hostsharing.hsadminng.hs.booking.project;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorRepository;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.api.HsBookingProjectsApi;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingProjectInsertResource;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingProjectPatchResource;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingProjectResource;
import net.hostsharing.hsadminng.mapper.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

@RestController
public class HsBookingProjectController implements HsBookingProjectsApi {

    @Autowired
    private Context context;

    @Autowired
    private Mapper mapper;

    @Autowired
    private HsBookingProjectRbacRepository bookingProjectRepo;

    @Autowired
    private HsBookingDebitorRepository debitorRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsBookingProjectResource>> listBookingProjectsByDebitorUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID debitorUuid) {
        context.define(currentSubject, assumedRoles);

        final var entities = bookingProjectRepo.findAllByDebitorUuid(debitorUuid);

        final var resources = mapper.mapList(entities, HsBookingProjectResource.class);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsBookingProjectResource> addBookingProject(
            final String currentSubject,
            final String assumedRoles,
            final HsBookingProjectInsertResource body) {

        context.define(currentSubject, assumedRoles);

        final var entityToSave = mapper.map(body, HsBookingProjectRbacEntity.class, RESOURCE_TO_ENTITY_POSTMAPPER);

        final var saved = bookingProjectRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/booking/projects/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsBookingProjectResource.class);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<HsBookingProjectResource> getBookingProjectByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID bookingProjectUuid) {

        context.define(currentSubject, assumedRoles);

        final var result = bookingProjectRepo.findByUuid(bookingProjectUuid);
        return result
                .map(bookingProjectEntity -> ResponseEntity.ok(
                        mapper.map(bookingProjectEntity, HsBookingProjectResource.class)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteBookingIemByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID bookingProjectUuid) {
        context.define(currentSubject, assumedRoles);

        final var result = bookingProjectRepo.deleteByUuid(bookingProjectUuid);
        return result == 0
                ? ResponseEntity.notFound().build()
                : ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<HsBookingProjectResource> patchBookingProject(
            final String currentSubject,
            final String assumedRoles,
            final UUID bookingProjectUuid,
            final HsBookingProjectPatchResource body) {

        context.define(currentSubject, assumedRoles);

        final var current = bookingProjectRepo.findByUuid(bookingProjectUuid).orElseThrow();

        new HsBookingProjectEntityPatcher(current).apply(body);

        final var saved = bookingProjectRepo.save(current);
        final var mapped = mapper.map(saved, HsBookingProjectResource.class);
        return ResponseEntity.ok(mapped);
    }

    final BiConsumer<HsBookingProjectInsertResource, HsBookingProjectRbacEntity> RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        if (resource.getDebitorUuid() != null) {
            entity.setDebitor(debitorRepo.findByUuid(resource.getDebitorUuid())
                    .orElseThrow(() -> new EntityNotFoundException("ERROR: [400] debitorUuid %s not found".formatted(
                            resource.getDebitorUuid()))));
        }
    };
}
