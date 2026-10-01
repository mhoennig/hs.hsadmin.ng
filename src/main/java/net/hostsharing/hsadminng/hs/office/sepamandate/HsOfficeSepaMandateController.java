package net.hostsharing.hsadminng.hs.office.sepamandate;

import lombok.val;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountIssues;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.validation.ValidationException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

import static net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity.DEBITOR_NUMBER_TAG;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.toPostgresDateRange;
import static net.hostsharing.hsadminng.repr.TaggedNumber.cropTag;

@RestController
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
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

    @Autowired
    private HsOfficeBankAccountIssues bankAccountIssues;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.sepaMandates.api.getListOfSepaMandates")
    public ResponseEntity<List<HsOfficeSepaMandateResource>> getListOfSepaMandates(
            final String assumedRoles,
            final String iban,
            final UUID debitorUuid,
            final String debitorNumber,
            final LocalDate validAt,
            final Boolean issues) {
        context.assumeRoles(assumedRoles);

        if (debitorUuid != null && debitorNumber != null) {
            throw new ValidationException("either debitorUuid or debitorNumber can be given, not both");
        }
        if (debitorNumber != null) {
            val debitor = debitorRepo.findDebitorByDebitorNumber(cropTag(DEBITOR_NUMBER_TAG, debitorNumber));
            if (debitor.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }
            return listOfSepaMandates(iban, debitor.get().getUuid(), validAt, issues);
        }
        return listOfSepaMandates(iban, debitorUuid, validAt, issues);
    }

    private ResponseEntity<List<HsOfficeSepaMandateResource>> listOfSepaMandates(
            final String iban, final UUID debitorUuid, final LocalDate validAt, final Boolean issues) {
        val onlyWithIssues = Boolean.TRUE.equals(issues);
        val resources = sepaMandateRepo.findSepaMandatesByOptionalIbanAndDebitorUuidAndValidAt(iban, debitorUuid, validAt).stream()
                .filter(entity -> !onlyWithIssues || validAt != null || isValidTodayOrLater(entity))
                .map(entity -> mapper.map(entity, HsOfficeSepaMandateResource.class, SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER))
                .toList();
        bankAccountIssues.addTo(resources.stream().map(HsOfficeSepaMandateResource::getBankAccount).filter(Objects::nonNull).toList());
        return ResponseEntity.ok(resources.stream()
                .filter(resource -> issues == null || issues == hasBankAccountIssues(resource))
                .toList());
    }

    // an empty validity, e.g. from validTo before validFrom, has no upper bound either, but is never valid
    private static boolean isValidTodayOrLater(final HsOfficeSepaMandateEntity entity) {
        val validity = entity.getValidity();
        return !validity.isEmpty() && (validity.upper() == null || validity.upper().isAfter(LocalDate.now()));
    }

    private static boolean hasBankAccountIssues(final HsOfficeSepaMandateResource resource) {
        return resource.getBankAccount() != null && resource.getBankAccount().getIssues() != null;
    }

    private HsOfficeSepaMandateResource toResource(final HsOfficeSepaMandateEntity entity) {
        val resource = mapper.map(entity, HsOfficeSepaMandateResource.class, SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER);
        bankAccountIssues.addTo(resource.getBankAccount());
        return resource;
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
        return ResponseEntity.created(uri).body(toResource(saved));
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
        return ResponseEntity.ok(toResource(result.get()));
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
        return ResponseEntity.ok(toResource(saved));
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
