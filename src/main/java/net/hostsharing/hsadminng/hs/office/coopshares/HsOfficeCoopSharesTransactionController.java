package net.hostsharing.hsadminng.hs.office.coopshares;

import jakarta.persistence.EntityNotFoundException;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeCoopSharesApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopSharesTransactionInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopSharesTransactionResource;
import net.hostsharing.hsadminng.errors.MultiValidationException;
import net.hostsharing.hsadminng.mapper.StandardMapper;
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

@RestController
public class HsOfficeCoopSharesTransactionController implements HsOfficeCoopSharesApi {

    @Autowired
    private Context context;

    @Autowired
    private StandardMapper mapper;

    @Autowired
    private HsOfficeCoopSharesTransactionRepository coopSharesTransactionRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficeCoopSharesTransactionResource>> getListOfCoopShares(
            final String currentSubject,
            final String assumedRoles,
            final UUID membershipUuid,
            final @DateTimeFormat(iso = ISO.DATE) LocalDate fromValueDate,
            final @DateTimeFormat(iso = ISO.DATE) LocalDate toValueDate) {
        context.define(currentSubject, assumedRoles);

        final var entities = coopSharesTransactionRepo.findCoopSharesTransactionByOptionalMembershipUuidAndDateRange(
                membershipUuid,
                fromValueDate,
                toValueDate);

        final var resources = mapper.mapList(entities, HsOfficeCoopSharesTransactionResource.class);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeCoopSharesTransactionResource> postNewCoopSharesTransaction(
            final String currentSubject,
            final String assumedRoles,
            final HsOfficeCoopSharesTransactionInsertResource requestBody) {

        context.define(currentSubject, assumedRoles);
        validate(requestBody);

        final var entityToSave = mapper.map(requestBody, HsOfficeCoopSharesTransactionEntity.class, RESOURCE_TO_ENTITY_POSTMAPPER);

        final var saved = coopSharesTransactionRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/coopsharestransactions/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsOfficeCoopSharesTransactionResource.class);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<HsOfficeCoopSharesTransactionResource> getSingleCoopShareTransactionByUuid(
        final String currentSubject, final String assumedRoles, final UUID shareTransactionUuid) {

            context.define(currentSubject, assumedRoles);

            final var result = coopSharesTransactionRepo.findByUuid(shareTransactionUuid);
            if (result.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(mapper.map(result.get(), HsOfficeCoopSharesTransactionResource.class));

    }

    private void validate(final HsOfficeCoopSharesTransactionInsertResource requestBody) {
        final var violations = new ArrayList<String>();
        validateSubscriptionTransaction(requestBody, violations);
        validateCancellationTransaction(requestBody, violations);
        validateshareCount(requestBody, violations);
        MultiValidationException.throwIfNotEmpty(violations);
    }

    private static void validateSubscriptionTransaction(
            final HsOfficeCoopSharesTransactionInsertResource requestBody,
            final ArrayList<String> violations) {
        if (requestBody.getTransactionType() == SUBSCRIPTION
                && requestBody.getShareCount() < 0) {
            violations.add("for %s, shareCount must be positive but is \"%d\"".formatted(
                    requestBody.getTransactionType(), requestBody.getShareCount()));
        }
    }

    private static void validateCancellationTransaction(
            final HsOfficeCoopSharesTransactionInsertResource requestBody,
            final ArrayList<String> violations) {
        if (requestBody.getTransactionType() == CANCELLATION
                && requestBody.getShareCount() > 0) {
            violations.add("for %s, shareCount must be negative but is \"%d\"".formatted(
                    requestBody.getTransactionType(), requestBody.getShareCount()));
        }
    }

    private static void validateshareCount(
            final HsOfficeCoopSharesTransactionInsertResource requestBody,
            final ArrayList<String> violations) {
        if (requestBody.getShareCount() == 0) {
            violations.add("shareCount must not be 0 but is \"%d\"".formatted(
                    requestBody.getShareCount()));
        }
    }

    final BiConsumer<HsOfficeCoopSharesTransactionInsertResource, HsOfficeCoopSharesTransactionEntity> RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        if ( resource.getRevertedShareTxUuid() != null ) {
            entity.setRevertedShareTx(coopSharesTransactionRepo.findByUuid(resource.getRevertedShareTxUuid())
                .orElseThrow(() -> new EntityNotFoundException("ERROR: [400] revertedShareTxUuid %s not found".formatted(resource.getRevertedShareTxUuid()))));
        }
    };
}
