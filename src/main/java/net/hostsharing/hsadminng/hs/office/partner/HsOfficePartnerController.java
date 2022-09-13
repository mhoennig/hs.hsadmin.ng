package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.Mapper;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficePartnersApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeContactResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerUpdateResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
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

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficePartnerResource>> listPartners(
            final String currentUser,
            final String assumedRoles,
            final String name) {
        // TODO.feat: context.define(currentUser, assumedRoles);

        // TODO.feat: final var entities = partnerRepo.findPartnerByOptionalNameLike(name);

        final var entities = List.of(
                HsOfficePartnerEntity.builder()
                        .uuid(UUID.randomUUID())
                        .person(HsOfficePersonEntity.builder()
                                .tradeName("Ixx AG")
                                .build())
                        .contact(HsOfficeContactEntity.builder()
                                .label("Ixx AG")
                                .build())
                        .build(),
                HsOfficePartnerEntity.builder()
                        .uuid(UUID.randomUUID())
                        .person(HsOfficePersonEntity.builder()
                                .tradeName("Ypsilon GmbH")
                                .build())
                        .contact(HsOfficeContactEntity.builder()
                                .label("Ypsilon GmbH")
                                .build())
                        .build(),
                HsOfficePartnerEntity.builder()
                        .uuid(UUID.randomUUID())
                        .person(HsOfficePersonEntity.builder()
                                .tradeName("Zett OHG")
                                .build())
                        .contact(HsOfficeContactEntity.builder()
                                .label("Zett OHG")
                                .build())
                        .build()
        );

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

        // TODO.feat: context.define(currentUser, assumedRoles);

        if (body.getUuid() == null) {
            body.setUuid(UUID.randomUUID());
        }

        // TODO.feat: final var saved = partnerRepo.save(map(body, HsOfficePartnerEntity.class));
        final var saved = map(body, HsOfficePartnerEntity.class, PARTNER_RESOURCE_TO_ENTITY_POSTMAPPER);

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
    public ResponseEntity<HsOfficePartnerResource> getPartnerByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID partnerUuid) {

        // TODO.feat: context.define(currentUser, assumedRoles);

        // TODO.feat: final var result = partnerRepo.findByUuid(partnerUuid);
        final var result =
                partnerUuid.equals(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6")) ? null :
                        HsOfficePartnerEntity.builder()
                                .uuid(UUID.randomUUID())
                                .person(HsOfficePersonEntity.builder()
                                        .tradeName("Ixx AG")
                                        .build())
                                .contact(HsOfficeContactEntity.builder()
                                        .label("Ixx AG")
                                        .build())
                                .build();
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(map(result, HsOfficePartnerResource.class, PARTNER_ENTITY_TO_RESOURCE_POSTMAPPER));
    }

    @Override
    public ResponseEntity<Void> deletePartnerByUuid(final String currentUser, final String assumedRoles, final UUID userUuid) {
        return null;
    }

    @Override
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
