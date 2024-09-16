package net.hostsharing.hsadminng.hs.office.coopassets;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeCoopAssetsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.*;
import net.hostsharing.hsadminng.errors.MultiValidationException;
import net.hostsharing.hsadminng.mapper.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

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
            final String currentSubject,
            final String assumedRoles,
            final UUID membershipUuid,
            final @DateTimeFormat(iso = ISO.DATE) LocalDate fromValueDate,
            final @DateTimeFormat(iso = ISO.DATE) LocalDate toValueDate) {
        context.define(currentSubject, assumedRoles);

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
            final String currentSubject,
            final String assumedRoles,
            final HsOfficeCoopAssetsTransactionInsertResource requestBody) {

        context.define(currentSubject, assumedRoles);
        validate(requestBody);

        final var entityToSave = mapper.map(requestBody, HsOfficeCoopAssetsTransactionEntity.class, RESOURCE_TO_ENTITY_POSTMAPPER);
        final var saved = coopAssetsTransactionRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/coopassetstransactions/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsOfficeCoopAssetsTransactionResource.class);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)

    public ResponseEntity<HsOfficeCoopAssetsTransactionResource> getCoopAssetTransactionByUuid(
        final String currentSubject, final String assumedRoles, final UUID assetTransactionUuid) {

        context.define(currentSubject, assumedRoles);

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
        MultiValidationException.throwIfNotEmpty(violations);
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

    final BiConsumer<HsOfficeCoopAssetsTransactionInsertResource, HsOfficeCoopAssetsTransactionEntity> RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        if ( resource.getReverseEntryUuid() != null ) {
            entity.setAdjustedAssetTx(coopAssetsTransactionRepo.findByUuid(resource.getReverseEntryUuid())
                    .orElseThrow(() -> new EntityNotFoundException("ERROR: [400] reverseEntityUuid %s not found".formatted(resource.getReverseEntryUuid()))));
        }
    };
};
