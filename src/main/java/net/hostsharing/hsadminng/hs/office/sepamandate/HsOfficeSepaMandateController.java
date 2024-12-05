package net.hostsharing.hsadminng.hs.office.sepamandate;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeSepaMandatesApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeSepaMandateInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeSepaMandatePatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeSepaMandateResource;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import static net.hostsharing.hsadminng.mapper.PostgresDateRange.toPostgresDateRange;

@RestController

public class HsOfficeSepaMandateController implements HsOfficeSepaMandatesApi {

    @Autowired
    private Context context;

    @Autowired
    private StandardMapper mapper;

    @Autowired
    private HsOfficeSepaMandateRepository sepaMandateRepo;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.sepaMandates.api.getListOfSepaMandates")
    public ResponseEntity<List<HsOfficeSepaMandateResource>> getListOfSepaMandates(
            final String currentSubject,
            final String assumedRoles,
            final String iban) {
        context.define(currentSubject, assumedRoles);

        final var entities = sepaMandateRepo.findSepaMandateByOptionalIban(iban);

        final var resources = mapper.mapList(entities, HsOfficeSepaMandateResource.class,
                SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    @Timed("app.office.sepaMandates.api.postNewSepaMandate")
    public ResponseEntity<HsOfficeSepaMandateResource> postNewSepaMandate(
            final String currentSubject,
            final String assumedRoles,
            final HsOfficeSepaMandateInsertResource body) {

        context.define(currentSubject, assumedRoles);

        final var entityToSave = mapper.map(body, HsOfficeSepaMandateEntity.class, SEPA_MANDATE_RESOURCE_TO_ENTITY_POSTMAPPER);

        final var saved = sepaMandateRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/sepamandates/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsOfficeSepaMandateResource.class,
                SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.sepaMandates.api.getSingleSepaMandateByUuid")
    public ResponseEntity<HsOfficeSepaMandateResource> getSingleSepaMandateByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID sepaMandateUuid) {

        context.define(currentSubject, assumedRoles);

        final var result = sepaMandateRepo.findByUuid(sepaMandateUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result.get(), HsOfficeSepaMandateResource.class,
                SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER));
    }

    @Override
    @Transactional
    @Timed("app.office.sepaMandates.api.deleteSepaMandateByUuid")
    public ResponseEntity<Void> deleteSepaMandateByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID sepaMandateUuid) {
        context.define(currentSubject, assumedRoles);

        final var result = sepaMandateRepo.deleteByUuid(sepaMandateUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    @Timed("app.office.sepaMandates.api.patchSepaMandate")
    public ResponseEntity<HsOfficeSepaMandateResource> patchSepaMandate(
            final String currentSubject,
            final String assumedRoles,
            final UUID sepaMandateUuid,
            final HsOfficeSepaMandatePatchResource body) {

        context.define(currentSubject, assumedRoles);

        final var current = sepaMandateRepo.findByUuid(sepaMandateUuid).orElseThrow();

        new HsOfficeSepaMandateEntityPatcher(current).apply(body);

        final var saved = sepaMandateRepo.save(current);
        final var mapped = mapper.map(saved, HsOfficeSepaMandateResource.class, SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(mapped);
    }

    final BiConsumer<HsOfficeSepaMandateEntity, HsOfficeSepaMandateResource> SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setValidFrom(entity.getValidity().lower());
        if (entity.getValidity().hasUpperBound()) {
            resource.setValidTo(entity.getValidity().upper().minusDays(1));
        }
        resource.getDebitor().setDebitorNumber(entity.getDebitor().getTaggedDebitorNumber());
    };

    final BiConsumer<HsOfficeSepaMandateInsertResource, HsOfficeSepaMandateEntity> SEPA_MANDATE_RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        entity.setValidity(toPostgresDateRange(resource.getValidFrom(), resource.getValidTo()));
    };
}
