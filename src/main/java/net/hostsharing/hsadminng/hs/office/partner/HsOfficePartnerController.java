package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.Mapper;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficePartnersApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeContactResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerUpdateResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
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
            final HsOfficePartnerResource body) {

        context.define(currentUser, assumedRoles);

        if (body.getUuid() == null) {
            body.setUuid(UUID.randomUUID());
        }

        final var entityToSave = map(body, HsOfficePartnerEntity.class);
        if (entityToSave.getContact().getUuid() != null) {
            contactRepo.findByUuid(entityToSave.getContact().getUuid()).ifPresent(entityToSave::setContact);
        } else {
            entityToSave.getContact().setUuid(UUID.randomUUID());
            entityToSave.setContact(contactRepo.save(entityToSave.getContact()));
        }
        if (entityToSave.getPerson().getUuid() != null) {
            personRepo.findByUuid(entityToSave.getPerson().getUuid()).ifPresent(entityToSave::setPerson);
        } else {
            entityToSave.getPerson().setUuid(UUID.randomUUID());
            entityToSave.setPerson(personRepo.save(entityToSave.getPerson()));
        }
        
        final var saved = partnerRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/partners/{id}")
                        .buildAndExpand(body.getUuid())
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
    public ResponseEntity<Void> deletePartnerByUuid(final String currentUser, final String assumedRoles, final UUID userUuid) {
        return null;
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficePartnerResource> updatePartner(
            final String currentUser,
            final String assumedRoles,
            final UUID partnerUuid,
            final HsOfficePartnerUpdateResource body) {
        return null;
    }

    private final BiConsumer<HsOfficePartnerResource, HsOfficePartnerEntity> PARTNER_RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        entity.setPerson(map(resource.getPerson(), HsOfficePersonEntity.class));
        entity.setContact(map(resource.getContact(), HsOfficeContactEntity.class));
    };

    private final BiConsumer<HsOfficePartnerEntity, HsOfficePartnerResource> PARTNER_ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setPerson(map(entity.getPerson(), HsOfficePersonResource.class));
        resource.setContact(map(entity.getContact(), HsOfficeContactResource.class));
    };

}
