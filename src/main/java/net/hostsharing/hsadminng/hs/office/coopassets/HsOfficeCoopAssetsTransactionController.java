package net.hostsharing.hsadminng.hs.office.coopassets;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeCoopAssetsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopAssetsTransactionInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopAssetsTransactionResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopSharesTransactionResource;
import net.hostsharing.hsadminng.mapper.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.validation.Valid;
import javax.validation.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.lang.String.join;
import static net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopAssetsTransactionTypeResource.*;

@RestController
public class HsOfficeCoopAssetsTransactionController implements HsOfficeCoopAssetsApi {

    @Autowired
    private Context context;

    @Autowired
    private Mapper mapper;

    @Autowired
    private HsOfficeCoopAssetsTransactionRepository coopAssetsTransactionRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficeCoopAssetsTransactionResource>> listCoopAssets(
            final String currentUser,
            final String assumedRoles,
            final UUID membershipUuid,
            final @DateTimeFormat(iso = ISO.DATE) LocalDate fromValueDate,
            final @DateTimeFormat(iso = ISO.DATE) LocalDate toValueDate) {
        context.define(currentUser, assumedRoles);

        final var entities = coopAssetsTransactionRepo.findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange(
                membershipUuid,
                fromValueDate,
                toValueDate);

        final var resources = mapper.mapList(entities, HsOfficeCoopAssetsTransactionResource.class);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeCoopAssetsTransactionResource> addCoopAssetsTransaction(
            final String currentUser,
            final String assumedRoles,
            @Valid final HsOfficeCoopAssetsTransactionInsertResource requestBody) {

        context.define(currentUser, assumedRoles);
        validate(requestBody);

        final var entityToSave = mapper.map(requestBody, HsOfficeCoopAssetsTransactionEntity.class);
        entityToSave.setUuid(UUID.randomUUID());

        final var saved = coopAssetsTransactionRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/coopassetstransactions/{id}")
                        .buildAndExpand(entityToSave.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsOfficeCoopAssetsTransactionResource.class);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)

    public ResponseEntity<HsOfficeCoopAssetsTransactionResource> getCoopAssetTransactionByUuid(
        final String currentUser, final String assumedRoles, final UUID assetTransactionUuid) {

        context.define(currentUser, assumedRoles);

        final var result = coopAssetsTransactionRepo.findByUuid(assetTransactionUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result.get(), HsOfficeCoopAssetsTransactionResource.class));

    }

    private void validate(final HsOfficeCoopAssetsTransactionInsertResource requestBody) {
        final var violations = new ArrayList<String>();
        validateDebitTransaction(requestBody, violations);
        validateCreditTransaction(requestBody, violations);
        validateAssetValue(requestBody, violations);
        if (violations.size() > 0) {
            throw new ValidationException("[" + join(", ", violations) + "]");
        }
    }

    private static void validateDebitTransaction(
            final HsOfficeCoopAssetsTransactionInsertResource requestBody,
            final ArrayList<String> violations) {
        if (List.of(DEPOSIT, ADOPTION).contains(requestBody.getTransactionType())
                && requestBody.getAssetValue().signum() < 0) {
            violations.add("for %s, assetValue must be positive but is \"%.2f\"".formatted(
                    requestBody.getTransactionType(), requestBody.getAssetValue()));
        }
    }

    private static void validateCreditTransaction(
            final HsOfficeCoopAssetsTransactionInsertResource requestBody,
            final ArrayList<String> violations) {
        if (List.of(DISBURSAL, TRANSFER, CLEARING, LOSS).contains(requestBody.getTransactionType())
                && requestBody.getAssetValue().signum() > 0) {
            violations.add("for %s, assetValue must be negative but is \"%.2f\"".formatted(
                    requestBody.getTransactionType(), requestBody.getAssetValue()));
        }
    }

    private static void validateAssetValue(
            final HsOfficeCoopAssetsTransactionInsertResource requestBody,
            final ArrayList<String> violations) {
        if (requestBody.getAssetValue().signum() == 0) {
            violations.add("assetValue must not be 0 but is \"%.2f\"".formatted(
                    requestBody.getAssetValue()));
        }
    }

}
