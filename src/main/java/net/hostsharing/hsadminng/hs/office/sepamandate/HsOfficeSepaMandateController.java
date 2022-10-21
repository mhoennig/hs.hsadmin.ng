package net.hostsharing.hsadminng.hs.office.sepamandate;

import com.vladmihalcea.hibernate.type.range.Range;
import net.hostsharing.hsadminng.mapper.Mapper;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeSepaMandatesApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeSepaMandateInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeSepaMandatePatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeSepaMandateResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.persistence.EntityManager;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import static net.hostsharing.hsadminng.mapper.Mapper.map;

@RestController

public class HsOfficeSepaMandateController implements HsOfficeSepaMandatesApi {

    @Autowired
    private Context context;

    @Autowired
    private HsOfficeSepaMandateRepository SepaMandateRepo;

    @Autowired
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficeSepaMandateResource>> listSepaMandatesByIban(
            final String currentUser,
            final String assumedRoles,
            final String iban) {
        context.define(currentUser, assumedRoles);

        final var entities = SepaMandateRepo.findSepaMandateByOptionalIban(iban);

        final var resources = Mapper.mapList(entities, HsOfficeSepaMandateResource.class,
                SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeSepaMandateResource> addSepaMandate(
            final String currentUser,
            final String assumedRoles,
            @Valid final HsOfficeSepaMandateInsertResource body) {

        context.define(currentUser, assumedRoles);

        final var entityToSave = map(body, HsOfficeSepaMandateEntity.class, SEPA_MANDATE_RESOURCE_TO_ENTITY_POSTMAPPER);
        entityToSave.setUuid(UUID.randomUUID());

        final var saved = SepaMandateRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/SepaMandates/{id}")
                        .buildAndExpand(entityToSave.getUuid())
                        .toUri();
        final var mapped = map(saved, HsOfficeSepaMandateResource.class,
                SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<HsOfficeSepaMandateResource> getSepaMandateByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID sepaMandateUuid) {

        context.define(currentUser, assumedRoles);

        final var result = SepaMandateRepo.findByUuid(sepaMandateUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(map(result.get(), HsOfficeSepaMandateResource.class,
                SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteSepaMandateByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID sepaMandateUuid) {
        context.define(currentUser, assumedRoles);

        final var result = SepaMandateRepo.deleteByUuid(sepaMandateUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeSepaMandateResource> patchSepaMandate(
            final String currentUser,
            final String assumedRoles,
            final UUID sepaMandateUuid,
            final HsOfficeSepaMandatePatchResource body) {

        context.define(currentUser, assumedRoles);

        final var current = SepaMandateRepo.findByUuid(sepaMandateUuid).orElseThrow();

        current.setValidity(toPostgresDateRange(current.getValidity().lower(), body.getValidTo()));

        final var saved = SepaMandateRepo.save(current);
        final var mapped = map(saved, HsOfficeSepaMandateResource.class, SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(mapped);
    }

    private static Range<LocalDate> toPostgresDateRange(
            final LocalDate validFrom,
            final LocalDate validTo) {
        return validTo != null
                ? Range.closedOpen(validFrom, validTo.plusDays(1))
                : Range.closedInfinite(validFrom);
    }

    final BiConsumer<HsOfficeSepaMandateEntity, HsOfficeSepaMandateResource> SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setValidFrom(entity.getValidity().lower());
        if (entity.getValidity().hasUpperBound()) {
            resource.setValidTo(entity.getValidity().upper().minusDays(1));
        }
    };

    final BiConsumer<HsOfficeSepaMandateInsertResource, HsOfficeSepaMandateEntity> SEPA_MANDATE_RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        entity.setValidity(toPostgresDateRange(resource.getValidFrom(), resource.getValidTo()));
    };
}
