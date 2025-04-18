package net.hostsharing.hsadminng.hs.office.coopshares;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.errors.MultiValidationException;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeCoopSharesApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopSharesTransactionInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopSharesTransactionResource;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipRepository;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import static net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopSharesTransactionTypeResource.CANCELLATION;
import static net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopSharesTransactionTypeResource.SUBSCRIPTION;
import static net.hostsharing.hsadminng.hs.validation.UuidResolver.resolve;

@RestController
@SecurityRequirement(name = "casTicket")
public class HsOfficeCoopSharesTransactionController implements HsOfficeCoopSharesApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private MessageTranslator messageTranslator;

    @Autowired
    private HsOfficeCoopSharesTransactionRepository coopSharesTransactionRepo;

    @Autowired
    private HsOfficeMembershipRepository membershipRepo;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.coopShares.api.getListOfCoopShares")
    public ResponseEntity<List<HsOfficeCoopSharesTransactionResource>> getListOfCoopShares(
            final String assumedRoles,
            final UUID membershipUuid,
            final @DateTimeFormat(iso = ISO.DATE) LocalDate fromValueDate,
            final @DateTimeFormat(iso = ISO.DATE) LocalDate toValueDate) {
        context.assumeRoles(assumedRoles);

        final var entities = coopSharesTransactionRepo.findCoopSharesTransactionByOptionalMembershipUuidAndDateRange(
                membershipUuid,
                fromValueDate,
                toValueDate);

        final var resources = mapper.mapList(
                entities,
                HsOfficeCoopSharesTransactionResource.class,
                ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    @Timed("app.office.coopShares.repo.postNewCoopSharesTransaction")
    public ResponseEntity<HsOfficeCoopSharesTransactionResource> postNewCoopSharesTransaction(
            final String assumedRoles,
            final HsOfficeCoopSharesTransactionInsertResource requestBody) {

        context.assumeRoles(assumedRoles);
        validate(requestBody);

        final var entityToSave = mapper.map(
                requestBody,
                HsOfficeCoopSharesTransactionEntity.class,
                RESOURCE_TO_ENTITY_POSTMAPPER);

        final var saved = coopSharesTransactionRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/coopsharestransactions/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsOfficeCoopSharesTransactionResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.coopShares.repo.getSingleCoopShareTransactionByUuid")
    public ResponseEntity<HsOfficeCoopSharesTransactionResource> getSingleCoopShareTransactionByUuid(
            final String assumedRoles, final UUID shareTransactionUuid) {

        context.assumeRoles(assumedRoles);

        final var result = coopSharesTransactionRepo.findByUuid(shareTransactionUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(
                result.get(),
                HsOfficeCoopSharesTransactionResource.class,
                ENTITY_TO_RESOURCE_POSTMAPPER));

    }

    private void validate(final HsOfficeCoopSharesTransactionInsertResource requestBody) {
        final var violations = new ArrayList<String>();
        validateSubscriptionTransaction(requestBody, violations);
        validateCancellationTransaction(requestBody, violations);
        validateshareCount(requestBody, violations);
        MultiValidationException.throwIfNotEmpty(violations);
    }

    private void validateSubscriptionTransaction(
            final HsOfficeCoopSharesTransactionInsertResource requestBody,
            final ArrayList<String> violations) {
        if (requestBody.getTransactionType() == SUBSCRIPTION
                && requestBody.getShareCount() < 0) {
            violations.add(messageTranslator.translate("for transactionType={0}, shareCount must be positive but is {1}",
                    requestBody.getTransactionType(), requestBody.getShareCount()));
        }
    }

    private void validateCancellationTransaction(
            final HsOfficeCoopSharesTransactionInsertResource requestBody,
            final ArrayList<String> violations) {
        if (requestBody.getTransactionType() == CANCELLATION
                && requestBody.getShareCount() > 0) {
            violations.add(messageTranslator.translate("for transactionType={0}, shareCount must be negative but is {1}",
                    requestBody.getTransactionType(), requestBody.getShareCount()));
        }
    }

    private void validateshareCount(
            final HsOfficeCoopSharesTransactionInsertResource requestBody,
            final ArrayList<String> violations) {
        if (requestBody.getShareCount() == 0) {
            violations.add(messageTranslator.translate("shareCount must not be 0"));
        }
    }

    final BiConsumer<HsOfficeCoopSharesTransactionInsertResource, HsOfficeCoopSharesTransactionEntity> RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        entity.setMembership(resolve("membership.uuid", resource.getMembershipUuid(), membershipRepo::findByUuid));
        if (resource.getRevertedShareTxUuid() != null) {
            entity.setRevertedShareTx(resolve(
                    "revertedShareTx.uuid",
                    resource.getRevertedShareTxUuid(),
                    coopSharesTransactionRepo::findByUuid));
        }
    };

    final BiConsumer<HsOfficeCoopSharesTransactionEntity, HsOfficeCoopSharesTransactionResource> ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setMembershipUuid(entity.getMembership().getUuid());
    };
}
