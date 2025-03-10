package net.hostsharing.hsadminng.hs.office.contact;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.mapper.StrictMapper;
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

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.errors.Validate.validate;

@RestController
public class HsOfficeContactController implements HsOfficeContactsApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private HsOfficeContactFromResourceConverter<HsOfficeContactRbacEntity> contactFromResourceConverter;

    @Autowired
    private HsOfficeContactRbacRepository contactRepo;

    @PostConstruct
    public void init() {
        // HOWTO: add a ModelMapper converter for a generic entity class to a ModelMapper to be used in a certain context
        //  This @PostConstruct could be implemented in the converter, but only without generics.
        //  But this converter is for HsOfficeContactRbacEntity and HsOfficeContactRealEntity.
        mapper.addConverter(contactFromResourceConverter, HsOfficeContactInsertResource.class, HsOfficeContactRbacEntity.class);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.contacts.api.getListOfContacts")
    public ResponseEntity<List<HsOfficeContactResource>> getListOfContacts(
            final String currentSubject,
            final String assumedRoles,
            final String caption,
            final String emailAddress) {
        context.define(currentSubject, assumedRoles);

        validate("caption, emailAddress").atMaxOne(caption, emailAddress);
        final var entities = emailAddress != null
             ? contactRepo.findContactByEmailAddress(emailAddress)
             : contactRepo.findContactByOptionalCaptionLike(caption);

        final var resources = mapper.mapList(entities, HsOfficeContactResource.class);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    @Timed("app.office.contacts.api.postNewContact")
    public ResponseEntity<HsOfficeContactResource> postNewContact(
            final String currentSubject,
            final String assumedRoles,
            final HsOfficeContactInsertResource body) {

        context.define(currentSubject, assumedRoles);

        final var entityToSave = mapper.map(body, HsOfficeContactRbacEntity.class);

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
    @Timed("app.office.contacts.api.getSingleContactByUuid")
    public ResponseEntity<HsOfficeContactResource> getSingleContactByUuid(
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
    @Timed("app.office.contacts.api.deleteContactByUuid")
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
    @Timed("app.office.contacts.api.patchContact")
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
}
