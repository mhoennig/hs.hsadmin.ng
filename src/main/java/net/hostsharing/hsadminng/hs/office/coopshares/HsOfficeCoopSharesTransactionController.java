package net.hostsharing.hsadminng.hs.office.coopshares;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeCoopSharesApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopSharesTransactionInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopSharesTransactionResource;
import net.hostsharing.hsadminng.mapper.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.lang.String.join;
import static net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopSharesTransactionTypeResource.CANCELLATION;
import static net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopSharesTransactionTypeResource.SUBSCRIPTION;

@RestController
public class HsOfficeCoopSharesTransactionController implements HsOfficeCoopSharesApi {

    @Autowired
    private Context context;

    @Autowired
    private Mapper mapper;

    @Autowired
    private HsOfficeCoopSharesTransactionRepository coopSharesTransactionRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficeCoopSharesTransactionResource>> listCoopShares(
            final String currentUser,
            final String assumedRoles,
            final UUID membershipUuid,
            final @DateTimeFormat(iso = ISO.DATE) LocalDate fromValueDate,
            final @DateTimeFormat(iso = ISO.DATE) LocalDate toValueDate) {
        context.define(currentUser, assumedRoles);

        final var entities = coopSharesTransactionRepo.findCoopSharesTransactionByOptionalMembershipUuidAndDateRange(
                membershipUuid,
                fromValueDate,
                toValueDate);

        final var resources = mapper.mapList(entities, HsOfficeCoopSharesTransactionResource.class);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeCoopSharesTransactionResource> addCoopSharesTransaction(
            final String currentUser,
            final String assumedRoles,
            @Valid final HsOfficeCoopSharesTransactionInsertResource requestBody) {

        context.define(currentUser, assumedRoles);
        validate(requestBody);

        final var entityToSave = mapper.map(requestBody, HsOfficeCoopSharesTransactionEntity.class);

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
    public ResponseEntity<HsOfficeCoopSharesTransactionResource> getCoopShareTransactionByUuid(
        final String currentUser, final String assumedRoles, final UUID shareTransactionUuid) {

            context.define(currentUser, assumedRoles);

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
        if (violations.size() > 0) {
            throw new ValidationException("[" + join(", ", violations) + "]");
        }
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

}
