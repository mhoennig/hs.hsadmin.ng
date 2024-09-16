package net.hostsharing.hsadminng.hs.office.contact;

import net.hostsharing.hsadminng.mapper.Mapper;
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
import java.util.function.BiConsumer;

import static net.hostsharing.hsadminng.mapper.KeyValueMap.from;

@RestController

public class HsOfficeContactController implements HsOfficeContactsApi {

    @Autowired
    private Context context;

    @Autowired
    private Mapper mapper;

    @Autowired
    private HsOfficeContactRbacRepository contactRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficeContactResource>> listContacts(
            final String currentSubject,
            final String assumedRoles,
            final String caption) {
        context.define(currentSubject, assumedRoles);

        final var entities = contactRepo.findContactByOptionalCaptionLike(caption);

        final var resources = mapper.mapList(entities, HsOfficeContactResource.class);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeContactResource> addContact(
            final String currentSubject,
            final String assumedRoles,
            final HsOfficeContactInsertResource body) {

        context.define(currentSubject, assumedRoles);

        final var entityToSave = mapper.map(body, HsOfficeContactRbacEntity.class, RESOURCE_TO_ENTITY_POSTMAPPER);

        final var saved = contactRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/contacts/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsOfficeContactResource.class);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<HsOfficeContactResource> getContactByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID contactUuid) {

        context.define(currentSubject, assumedRoles);

        final var result = contactRepo.findByUuid(contactUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result.get(), HsOfficeContactResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteContactByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID contactUuid) {
        context.define(currentSubject, assumedRoles);

        final var result = contactRepo.deleteByUuid(contactUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeContactResource> patchContact(
            final String currentSubject,
            final String assumedRoles,
            final UUID contactUuid,
            final HsOfficeContactPatchResource body) {

        context.define(currentSubject, assumedRoles);

        final var current = contactRepo.findByUuid(contactUuid).orElseThrow();

        new HsOfficeContactEntityPatcher(current).apply(body);

        final var saved = contactRepo.save(current);
        final var mapped = mapper.map(saved, HsOfficeContactResource.class);
        return ResponseEntity.ok(mapped);
    }

    @SuppressWarnings("unchecked")
    final BiConsumer<HsOfficeContactInsertResource, HsOfficeContactRbacEntity> RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        entity.putEmailAddresses(from(resource.getEmailAddresses()));
        entity.putPhoneNumbers(from(resource.getPhoneNumbers()));
    };
}
