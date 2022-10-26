package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficePartnersApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerResource;
import net.hostsharing.hsadminng.mapper.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.UUID;

@RestController

public class HsOfficePartnerController implements HsOfficePartnersApi {

    @Autowired
    private Context context;

    @Autowired
    private Mapper mapper;

    @Autowired
    private HsOfficePartnerRepository partnerRepo;

    @Autowired
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficePartnerResource>> listPartners(
            final String currentUser,
            final String assumedRoles,
            final String name) {
        context.define(currentUser, assumedRoles);

        final var entities = partnerRepo.findPartnerByOptionalNameLike(name);

        final var resources = mapper.mapList(entities, HsOfficePartnerResource.class);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficePartnerResource> addPartner(
            final String currentUser,
            final String assumedRoles,
            final HsOfficePartnerInsertResource body) {

        context.define(currentUser, assumedRoles);

        final var entityToSave = mapper.map(body, HsOfficePartnerEntity.class);

        final var saved = partnerRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/partners/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsOfficePartnerResource.class);
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
        return ResponseEntity.ok(mapper.map(result.get(), HsOfficePartnerResource.class));
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

        new HsOfficePartnerEntityPatcher(em, current).apply(body);

        final var saved = partnerRepo.save(current);
        final var mapped = mapper.map(saved, HsOfficePartnerResource.class);
        return ResponseEntity.ok(mapped);
    }
}
