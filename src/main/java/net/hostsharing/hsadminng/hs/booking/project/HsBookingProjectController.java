package net.hostsharing.hsadminng.hs.booking.project;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorRepository;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.api.HsBookingProjectsApi;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingProjectInsertResource;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingProjectPatchResource;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingProjectResource;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

@RestController
@Profile("!only-prod-schema")
@SecurityRequirement(name = "casTicket")
public class HsBookingProjectController implements HsBookingProjectsApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private HsBookingProjectRbacRepository bookingProjectRepo;

    @Autowired
    private HsBookingDebitorRepository debitorRepo;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.bookingProjects.api.getListOfBookingProjectsByDebitorUuid")
    public ResponseEntity<List<HsBookingProjectResource>> getListOfBookingProjectsByDebitorUuid(
            final String assumedRoles,
            final UUID debitorUuid) {
        context.assumeRoles(assumedRoles);

        final var entities = bookingProjectRepo.findAllByDebitorUuid(debitorUuid);

        final var resources = mapper.mapList(entities, HsBookingProjectResource.class);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    @Timed("app.bookingProjects.api.postNewBookingProject")
    public ResponseEntity<HsBookingProjectResource> postNewBookingProject(
            final String assumedRoles,
            final HsBookingProjectInsertResource body) {

        context.assumeRoles(assumedRoles);

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
    @Timed("app.bookingProjects.api.getBookingProjectByUuid")
    public ResponseEntity<HsBookingProjectResource> getBookingProjectByUuid(
            final String assumedRoles,
            final UUID bookingProjectUuid) {

        context.assumeRoles(assumedRoles);

        final var result = bookingProjectRepo.findByUuid(bookingProjectUuid);
        return result
                .map(bookingProjectEntity -> ResponseEntity.ok(
                        mapper.map(bookingProjectEntity, HsBookingProjectResource.class)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    @Transactional
    @Timed("app.bookingProjects.api.deleteBookingIemByUuid")
    public ResponseEntity<Void> deleteBookingIemByUuid(
            final String assumedRoles,
            final UUID bookingProjectUuid) {
        context.assumeRoles(assumedRoles);

        final var result = bookingProjectRepo.deleteByUuid(bookingProjectUuid);
        return result == 0
                ? ResponseEntity.notFound().build()
                : ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    @Timed("app.bookingProjects.api.patchBookingProject")
    public ResponseEntity<HsBookingProjectResource> patchBookingProject(
            final String assumedRoles,
            final UUID bookingProjectUuid,
            final HsBookingProjectPatchResource body) {

        context.assumeRoles(assumedRoles);

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
