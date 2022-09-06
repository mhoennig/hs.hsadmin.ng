package net.hostsharing.hsadminng.hs.admin.partner;

import net.hostsharing.hsadminng.Mapper;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.admin.contact.HsAdminContactEntity;
import net.hostsharing.hsadminng.hs.admin.generated.api.v1.api.HsAdminPartnersApi;
import net.hostsharing.hsadminng.hs.admin.generated.api.v1.model.HsAdminContactResource;
import net.hostsharing.hsadminng.hs.admin.generated.api.v1.model.HsAdminPartnerResource;
import net.hostsharing.hsadminng.hs.admin.generated.api.v1.model.HsAdminPartnerUpdateResource;
import net.hostsharing.hsadminng.hs.admin.generated.api.v1.model.HsAdminPersonResource;
import net.hostsharing.hsadminng.hs.admin.person.HsAdminPersonEntity;
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

public class HsAdminPartnerController implements HsAdminPartnersApi {

    @Autowired
    private Context context;

    @Autowired
    private HsAdminPartnerRepository partnerRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsAdminPartnerResource>> listPartners(
            final String currentUser,
            final String assumedRoles,
            final String name) {
        // TODO: context.define(currentUser, assumedRoles);

        // TODO: final var entities = partnerRepo.findPartnerByOptionalNameLike(name);

        final var entities = List.of(
                HsAdminPartnerEntity.builder()
                        .uuid(UUID.randomUUID())
                        .person(HsAdminPersonEntity.builder()
                                .tradeName("Ixx AG")
                                .build())
                        .contact(HsAdminContactEntity.builder()
                                .label("Ixx AG")
                                .build())
                        .build(),
                HsAdminPartnerEntity.builder()
                        .uuid(UUID.randomUUID())
                        .person(HsAdminPersonEntity.builder()
                                .tradeName("Ypsilon GmbH")
                                .build())
                        .contact(HsAdminContactEntity.builder()
                                .label("Ypsilon GmbH")
                                .build())
                        .build(),
                HsAdminPartnerEntity.builder()
                        .uuid(UUID.randomUUID())
                        .person(HsAdminPersonEntity.builder()
                                .tradeName("Zett OHG")
                                .build())
                        .contact(HsAdminContactEntity.builder()
                                .label("Zett OHG")
                                .build())
                        .build()
        );

        final var resources = Mapper.mapList(entities, HsAdminPartnerResource.class,
                PARTNER_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsAdminPartnerResource> addPartner(
            final String currentUser,
            final String assumedRoles,
            final HsAdminPartnerResource body) {

        // TODO: context.define(currentUser, assumedRoles);

        if (body.getUuid() == null) {
            body.setUuid(UUID.randomUUID());
        }

        // TODO: final var saved = partnerRepo.save(map(body, HsAdminPartnerEntity.class));
        final var saved = map(body, HsAdminPartnerEntity.class, PARTNER_RESOURCE_TO_ENTITY_POSTMAPPER);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/admin/partners/{id}")
                        .buildAndExpand(body.getUuid())
                        .toUri();
        final var mapped = map(saved, HsAdminPartnerResource.class,
                PARTNER_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    public ResponseEntity<HsAdminPartnerResource> getPartnerByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID partnerUuid) {

        // TODO: context.define(currentUser, assumedRoles);

        // TODO: final var result = partnerRepo.findByUuid(partnerUuid);
        final var result =
                partnerUuid.equals(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6")) ? null :
                        HsAdminPartnerEntity.builder()
                                .uuid(UUID.randomUUID())
                                .person(HsAdminPersonEntity.builder()
                                        .tradeName("Ixx AG")
                                        .build())
                                .contact(HsAdminContactEntity.builder()
                                        .label("Ixx AG")
                                        .build())
                                .build();
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(map(result, HsAdminPartnerResource.class, PARTNER_ENTITY_TO_RESOURCE_POSTMAPPER));
    }

    @Override
    public ResponseEntity<Void> deletePartnerByUuid(final String currentUser, final String assumedRoles, final UUID userUuid) {
        return null;
    }

    @Override
    public ResponseEntity<HsAdminPartnerResource> updatePartner(
            final String currentUser,
            final String assumedRoles,
            final UUID partnerUuid,
            final HsAdminPartnerUpdateResource body) {
        return null;
    }

    private final BiConsumer<HsAdminPartnerResource, HsAdminPartnerEntity> PARTNER_RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        entity.setPerson(map(resource.getPerson(), HsAdminPersonEntity.class));
        entity.setContact(map(resource.getContact(), HsAdminContactEntity.class));
    };

    private final BiConsumer<HsAdminPartnerEntity, HsAdminPartnerResource> PARTNER_ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setPerson(map(entity.getPerson(), HsAdminPersonResource.class));
        resource.setContact(map(entity.getContact(), HsAdminContactResource.class));
    };

}
