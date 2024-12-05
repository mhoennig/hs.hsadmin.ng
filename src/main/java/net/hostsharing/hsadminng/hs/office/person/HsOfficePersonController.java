package net.hostsharing.hsadminng.hs.office.person;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficePersonsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController

public class HsOfficePersonController implements HsOfficePersonsApi {

    @Autowired
    private Context context;

    @Autowired
    private StandardMapper mapper;

    @Autowired
    private HsOfficePersonRepository personRepo;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.persons.api.getListOfPersons")
    public ResponseEntity<List<HsOfficePersonResource>> getListOfPersons(
            final String currentSubject,
            final String assumedRoles,
            final String caption) {
        context.define(currentSubject, assumedRoles);

        final var entities = personRepo.findPersonByOptionalNameLike(caption);

        final var resources = mapper.mapList(entities, HsOfficePersonResource.class);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    @Timed("app.office.persons.api.postNewPerson")
    public ResponseEntity<HsOfficePersonResource> postNewPerson(
            final String currentSubject,
            final String assumedRoles,
            final HsOfficePersonInsertResource body) {

        context.define(currentSubject, assumedRoles);

        final var entityToSave = mapper.map(body, HsOfficePersonEntity.class);

        final var saved = personRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/persons/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsOfficePersonResource.class);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.persons.api.getSinglePersonByUuid")
    public ResponseEntity<HsOfficePersonResource> getSinglePersonByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID personUuid) {

        context.define(currentSubject, assumedRoles);

        final var result = personRepo.findByUuid(personUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result.get(), HsOfficePersonResource.class));
    }

    @Override
    @Transactional
    @Timed("app.office.persons.api.deletePersonByUuid")
    public ResponseEntity<Void> deletePersonByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID personUuid) {
        context.define(currentSubject, assumedRoles);

        final var result = personRepo.deleteByUuid(personUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    @Timed("app.office.persons.api.patchPerson")
    public ResponseEntity<HsOfficePersonResource> patchPerson(
            final String currentSubject,
            final String assumedRoles,
            final UUID personUuid,
            final HsOfficePersonPatchResource body) {

        context.define(currentSubject, assumedRoles);

        final var current = personRepo.findByUuid(personUuid).orElseThrow();

        new HsOfficePersonEntityPatcher(current).apply(body);

        final var saved = personRepo.save(current);
        final var mapped = mapper.map(saved, HsOfficePersonResource.class);
        return ResponseEntity.ok(mapped);
    }
}
