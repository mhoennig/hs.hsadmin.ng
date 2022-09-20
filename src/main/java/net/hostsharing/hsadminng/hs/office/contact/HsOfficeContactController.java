package net.hostsharing.hsadminng.hs.office.contact;

import net.hostsharing.hsadminng.Mapper;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeContactsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeContactInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeContactPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeContactResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.Mapper.map;

@RestController

public class HsOfficeContactController implements HsOfficeContactsApi {

    @Autowired
    private Context context;

    @Autowired
    private HsOfficeContactRepository contactRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficeContactResource>> listContacts(
            final String currentUser,
            final String assumedRoles,
            final String label) {
        context.define(currentUser, assumedRoles);

        final var entities = contactRepo.findContactByOptionalLabelLike(label);

        final var resources = Mapper.mapList(entities, HsOfficeContactResource.class);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeContactResource> addContact(
            final String currentUser,
            final String assumedRoles,
            final HsOfficeContactInsertResource body) {

        context.define(currentUser, assumedRoles);

        final var entityToSave = map(body, HsOfficeContactEntity.class);
        entityToSave.setUuid(UUID.randomUUID());

        final var saved = contactRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/contacts/{id}")
                        .buildAndExpand(entityToSave.getUuid())
                        .toUri();
        final var mapped = map(saved, HsOfficeContactResource.class);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<HsOfficeContactResource> getContactByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID contactUuid) {

        context.define(currentUser, assumedRoles);

        final var result = contactRepo.findByUuid(contactUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(map(result.get(), HsOfficeContactResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteContactByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID contactUuid) {
        context.define(currentUser, assumedRoles);

        final var result = contactRepo.deleteByUuid(contactUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeContactResource> patchContact(
            final String currentUser,
            final String assumedRoles,
            final UUID contactUuid,
            final HsOfficeContactPatchResource body) {

        context.define(currentUser, assumedRoles);

        final var current = contactRepo.findByUuid(contactUuid).orElseThrow();

        new HsOfficeContactEntityPatch(current).apply(body);

        final var saved = contactRepo.save(current);
        final var mapped = map(saved, HsOfficeContactResource.class);
        return ResponseEntity.ok(mapped);
    }
}
