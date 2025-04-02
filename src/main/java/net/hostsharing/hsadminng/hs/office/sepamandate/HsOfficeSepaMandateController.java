package net.hostsharing.hsadminng.hs.office.sepamandate;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountRepository;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeSepaMandatesApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeDebitorResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeSepaMandateInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeSepaMandatePatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeSepaMandateResource;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.validation.ValidationException;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import static net.hostsharing.hsadminng.mapper.PostgresDateRange.toPostgresDateRange;

@RestController
@SecurityRequirement(name = "casTicket")
public class HsOfficeSepaMandateController implements HsOfficeSepaMandatesApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private HsOfficeDebitorRepository debitorRepo;

    @Autowired
    private HsOfficeBankAccountRepository bankAccountRepo;

    @Autowired
    private HsOfficeSepaMandateRepository sepaMandateRepo;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.sepaMandates.api.getListOfSepaMandates")
    public ResponseEntity<List<HsOfficeSepaMandateResource>> getListOfSepaMandates(
            final String assumedRoles,
            final String iban) {
        context.assumeRoles(assumedRoles);

        final var entities = sepaMandateRepo.findSepaMandateByOptionalIban(iban);

        final var resources = mapper.mapList(entities, HsOfficeSepaMandateResource.class,
                SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    @Timed("app.office.sepaMandates.api.postNewSepaMandate")
    public ResponseEntity<HsOfficeSepaMandateResource> postNewSepaMandate(
            final String assumedRoles,
            final HsOfficeSepaMandateInsertResource body) {

        context.assumeRoles(assumedRoles);

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
            final String assumedRoles,
            final UUID sepaMandateUuid) {

        context.assumeRoles(assumedRoles);

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
            final String assumedRoles,
            final UUID sepaMandateUuid) {
        context.assumeRoles(assumedRoles);

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
            final String assumedRoles,
            final UUID sepaMandateUuid,
            final HsOfficeSepaMandatePatchResource body) {

        context.assumeRoles(assumedRoles);

        final var current = sepaMandateRepo.findByUuid(sepaMandateUuid).orElseThrow();

        new HsOfficeSepaMandateEntityPatcher(current).apply(body);

        final var saved = sepaMandateRepo.save(current);
        final var mapped = mapper.map(saved, HsOfficeSepaMandateResource.class, SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(mapped);
    }

    final BiConsumer<HsOfficeSepaMandateEntity, HsOfficeSepaMandateResource> SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setValidFrom(entity.getValidity().lower());
         if (entity.getValidity().upper() != null) {
            resource.setValidTo(entity.getValidity().upper().minusDays(1));
        }
        resource.setDebitor(mapper.map(entity.getDebitor(), HsOfficeDebitorResource.class));
        resource.getDebitor().setDebitorNumber(entity.getDebitor().getTaggedDebitorNumber());
        resource.getDebitor().getPartner().setPartnerNumber(entity.getDebitor().getPartner().getTaggedPartnerNumber());
    };

    final BiConsumer<HsOfficeSepaMandateInsertResource, HsOfficeSepaMandateEntity> SEPA_MANDATE_RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        entity.setValidity(toPostgresDateRange(resource.getValidFrom(), resource.getValidTo()));
        entity.setDebitor(debitorRepo.findByUuid(resource.getDebitorUuid()).orElseThrow( () ->
                new ValidationException(
                        "debitor.uuid='" + resource.getDebitorUuid() + "' not found or not accessible"
                )
        ));
        entity.setBankAccount(bankAccountRepo.findByUuid(resource.getBankAccountUuid()).orElseThrow( () ->
                new ValidationException(
                        "bankAccount.uuid='" + resource.getBankAccountUuid() + "' not found or not accessible"
                )
        ));
    };
}
