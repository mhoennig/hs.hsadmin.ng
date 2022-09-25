package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.Mapper;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficePartnersApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.*;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.BiConsumer;

import static net.hostsharing.hsadminng.Mapper.map;

@RestController

public class HsOfficePartnerController implements HsOfficePartnersApi {

    @Autowired
    private Context context;

    @Autowired
    private HsOfficePartnerRepository partnerRepo;

    @Autowired
    private HsOfficePersonRepository personRepo;

    @Autowired
    private HsOfficeContactRepository contactRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficePartnerResource>> listPartners(
            final String currentUser,
            final String assumedRoles,
            final String name) {
        context.define(currentUser, assumedRoles);

        final var entities = partnerRepo.findPartnerByOptionalNameLike(name);

        final var resources = Mapper.mapList(entities, HsOfficePartnerResource.class,
                PARTNER_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficePartnerResource> addPartner(
            final String currentUser,
            final String assumedRoles,
            final HsOfficePartnerInsertResource body) {

        context.define(currentUser, assumedRoles);

        final var entityToSave = mapToHsOfficePartnerEntity(body);
        entityToSave.setUuid(UUID.randomUUID());
        entityToSave.setContact(contactRepo.findByUuid(body.getContactUuid()).orElseThrow(
                () -> new NoSuchElementException("cannot find contact uuid " + body.getContactUuid())
        ));
        entityToSave.setPerson(personRepo.findByUuid(body.getPersonUuid()).orElseThrow(
                () -> new NoSuchElementException("cannot find person uuid " + body.getPersonUuid())
        ));

        final var saved = partnerRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/partners/{id}")
                        .buildAndExpand(entityToSave.getUuid())
                        .toUri();
        final var mapped = map(saved, HsOfficePartnerResource.class,
                PARTNER_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<HsOfficePartnerResource> getPartnerByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID partnerUuid) {

        context.define(currentUser, assumedRoles);

        final var result = partnerRepo.findByUuid(partnerUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(map(result.get(), HsOfficePartnerResource.class, PARTNER_ENTITY_TO_RESOURCE_POSTMAPPER));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deletePartnerByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID partnerUuid) {
        context.define(currentUser, assumedRoles);

        final var result = partnerRepo.deleteByUuid(partnerUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficePartnerResource> patchPartner(
            final String currentUser,
            final String assumedRoles,
            final UUID partnerUuid,
            final HsOfficePartnerPatchResource body) {

        context.define(currentUser, assumedRoles);

        final var current = partnerRepo.findByUuid(partnerUuid).orElseThrow();

        new HsOfficePartnerEntityPatcher(current, contactRepo::findByUuid, personRepo::findByUuid).apply(body);

        final var saved = partnerRepo.save(current);
        final var mapped = map(saved, HsOfficePartnerResource.class);
        return ResponseEntity.ok(mapped);
    }


    final BiConsumer<HsOfficePartnerEntity, HsOfficePartnerResource> PARTNER_ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setPerson(map(entity.getPerson(), HsOfficePersonResource.class));
        resource.setContact(map(entity.getContact(), HsOfficeContactResource.class));
    };

    private HsOfficePartnerEntity mapToHsOfficePartnerEntity(final HsOfficePartnerInsertResource resource) {
        final var entity = new HsOfficePartnerEntity();
        entity.setBirthday(resource.getBirthday());
        entity.setBirthName(resource.getBirthName());
        entity.setDateOfDeath(resource.getDateOfDeath());
        entity.setRegistrationNumber(resource.getRegistrationNumber());
        entity.setRegistrationOffice(resource.getRegistrationOffice());
        return entity;
    }
}
