package net.hostsharing.hsadminng.hs.office.coopassets;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.errors.MultiValidationException;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeCoopAssetsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopAssetsTransactionInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopAssetsTransactionResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopAssetsTransactionTypeResource;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipRepository;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hs.office.coopassets.HsOfficeCoopAssetsTransactionType.REVERSAL;
import static net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopAssetsTransactionTypeResource.CLEARING;
import static net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopAssetsTransactionTypeResource.DEPOSIT;
import static net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopAssetsTransactionTypeResource.DISBURSAL;
import static net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopAssetsTransactionTypeResource.LOSS;
import static net.hostsharing.hsadminng.lambda.WithNonNull.withNonNull;

@RestController
@SecurityRequirement(name = "casTicket")
public class HsOfficeCoopAssetsTransactionController implements HsOfficeCoopAssetsApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private EntityManagerWrapper emw;

    @Autowired
    private MessageTranslator messageTranslator;

    @Autowired
    private HsOfficeCoopAssetsTransactionRepository coopAssetsTransactionRepo;

    @Autowired
    private HsOfficeMembershipRepository membershipRepo;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.coopAssets.api.getListOfCoopAssets")
    public ResponseEntity<List<HsOfficeCoopAssetsTransactionResource>> getListOfCoopAssets(
            final String assumedRoles,
            final UUID membershipUuid,
            final @DateTimeFormat(iso = ISO.DATE) LocalDate fromValueDate,
            final @DateTimeFormat(iso = ISO.DATE) LocalDate toValueDate) {
        context.assumeRoles(assumedRoles);

        final var entities = coopAssetsTransactionRepo.findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange(
                membershipUuid,
                fromValueDate,
                toValueDate);

        final var resources = mapper.mapList(
                entities,
                HsOfficeCoopAssetsTransactionResource.class,
                ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    @Timed("app.office.coopAssets.api.postNewCoopAssetTransaction")
    public ResponseEntity<HsOfficeCoopAssetsTransactionResource> postNewCoopAssetTransaction(
            final String assumedRoles,
            final HsOfficeCoopAssetsTransactionInsertResource requestBody) {

        context.assumeRoles(assumedRoles);
        validate(requestBody);

        final var entityToSave = mapper.map(
                requestBody,
                HsOfficeCoopAssetsTransactionEntity.class,
                RESOURCE_TO_ENTITY_POSTMAPPER);
        final var saved = coopAssetsTransactionRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/coopassetstransactions/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsOfficeCoopAssetsTransactionResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.coopAssets.api.getSingleCoopAssetTransactionByUuid")
    public ResponseEntity<HsOfficeCoopAssetsTransactionResource> getSingleCoopAssetTransactionByUuid(
            final String assumedRoles, final UUID assetTransactionUuid) {

        context.assumeRoles(assumedRoles);

        final var result = coopAssetsTransactionRepo.findByUuid(assetTransactionUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        final var resource = mapper.map(
                result.get(),
                HsOfficeCoopAssetsTransactionResource.class,
                ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resource);

    }

    private void validate(final HsOfficeCoopAssetsTransactionInsertResource requestBody) {
        final var violations = new ArrayList<String>();
        validateDebitTransaction(requestBody, violations);
        validateCreditTransaction(requestBody, violations);
        validateAssetValue(requestBody, violations);
        MultiValidationException.throwIfNotEmpty(violations);
    }

    private void validateDebitTransaction(
            final HsOfficeCoopAssetsTransactionInsertResource requestBody,
            final ArrayList<String> violations) {
        if (List.of(DEPOSIT, HsOfficeCoopAssetsTransactionTypeResource.ADOPTION).contains(requestBody.getTransactionType())
                && requestBody.getAssetValue().signum() < 0) {
            violations.add(messageTranslator.translate("for transactionType={0}, assetValue must be positive but is {1,number,#0.00}",
                    requestBody.getTransactionType(), requestBody.getAssetValue()));
        }
    }

    private void validateCreditTransaction(
            final HsOfficeCoopAssetsTransactionInsertResource requestBody,
            final ArrayList<String> violations) {
        if (List.of(DISBURSAL, HsOfficeCoopAssetsTransactionTypeResource.TRANSFER, CLEARING, LOSS)
                .contains(requestBody.getTransactionType())
                && requestBody.getAssetValue().signum() > 0) {
            violations.add(messageTranslator.translate("for transactionType={0}, assetValue must be negative but is {1,number,#0.00}",
                    requestBody.getTransactionType(), requestBody.getAssetValue()));
        }
    }

    private void validateAssetValue(
            final HsOfficeCoopAssetsTransactionInsertResource requestBody,
            final ArrayList<String> violations) {
        if (requestBody.getAssetValue().signum() == 0) {
            violations.add(messageTranslator.translate("assetValue must not be 0"));
        }
    }

    // TODO.refa: this logic needs to get extracted to a service
    final BiConsumer<HsOfficeCoopAssetsTransactionEntity, HsOfficeCoopAssetsTransactionResource> ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setMembershipUuid(entity.getMembership().getUuid());
        resource.setMembershipMemberNumber(entity.getMembership().getTaggedMemberNumber());

        withNonNull(
                resource.getReversalAssetTx(), reversalAssetTxResource -> {
                    reversalAssetTxResource.setMembershipUuid(entity.getMembership().getUuid());
                    reversalAssetTxResource.setMembershipMemberNumber(entity.getTaggedMemberNumber());
                    reversalAssetTxResource.setRevertedAssetTxUuid(entity.getUuid());
                    withNonNull(
                            entity.getAdoptionAssetTx(), adoptionAssetTx ->
                                    reversalAssetTxResource.setAdoptionAssetTxUuid(adoptionAssetTx.getUuid()));
                    withNonNull(
                            entity.getTransferAssetTx(), transferAssetTxResource ->
                                    reversalAssetTxResource.setTransferAssetTxUuid(transferAssetTxResource.getUuid()));
                });

        withNonNull(
                resource.getRevertedAssetTx(), revertAssetTxResource -> {
                    revertAssetTxResource.setMembershipUuid(entity.getMembership().getUuid());
                    revertAssetTxResource.setMembershipMemberNumber(entity.getTaggedMemberNumber());
                    revertAssetTxResource.setReversalAssetTxUuid(entity.getUuid());
                    withNonNull(
                            entity.getRevertedAssetTx().getAdoptionAssetTx(), adoptionAssetTx ->
                                    revertAssetTxResource.setAdoptionAssetTxUuid(adoptionAssetTx.getUuid()));
                    withNonNull(
                            entity.getRevertedAssetTx().getTransferAssetTx(), transferAssetTxResource ->
                                    revertAssetTxResource.setTransferAssetTxUuid(transferAssetTxResource.getUuid()));
                });

        withNonNull(
                resource.getAdoptionAssetTx(), adoptionAssetTxResource -> {
                    adoptionAssetTxResource.setMembershipUuid(entity.getAdoptionAssetTx().getMembership().getUuid());
                    adoptionAssetTxResource.setMembershipMemberNumber(entity.getAdoptionAssetTx().getTaggedMemberNumber());
                    adoptionAssetTxResource.setTransferAssetTxUuid(entity.getUuid());
                    withNonNull(
                            entity.getAdoptionAssetTx().getReversalAssetTx(), reversalAssetTx ->
                                    adoptionAssetTxResource.setReversalAssetTxUuid(reversalAssetTx.getUuid()));
                });

        withNonNull(
                resource.getTransferAssetTx(), transferAssetTxResource -> {
                    resource.getTransferAssetTx().setMembershipUuid(entity.getTransferAssetTx().getMembership().getUuid());
                    resource.getTransferAssetTx()
                            .setMembershipMemberNumber(entity.getTransferAssetTx().getTaggedMemberNumber());
                    resource.getTransferAssetTx().setAdoptionAssetTxUuid(entity.getUuid());
                    withNonNull(
                            entity.getTransferAssetTx().getReversalAssetTx(), reversalAssetTx ->
                                    transferAssetTxResource.setReversalAssetTxUuid(reversalAssetTx.getUuid()));
                });
    };

    // TODO.refa: this logic needs to get extracted to a service
    final BiConsumer<HsOfficeCoopAssetsTransactionInsertResource, HsOfficeCoopAssetsTransactionEntity> RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {

        if (resource.getMembershipUuid() != null) {
            final HsOfficeMembershipEntity membership = ofNullable(emw.find(
                    HsOfficeMembershipEntity.class,
                    resource.getMembershipUuid()))
                    .orElseThrow(() -> new EntityNotFoundException(
                            messageTranslator.translate(
                                "{0} \"{1}\" not found", "membership.uuid", resource.getMembershipUuid())));
            entity.setMembership(membership);
        }

        if (entity.getTransactionType() == REVERSAL) {
            if (resource.getRevertedAssetTxUuid() == null) {
                throw new ValidationException(messageTranslator.translate(
                        "a REVERSAL asset transaction requires specifying a revertedAssetTx.uuid"));
            }
            final var revertedAssetTx = coopAssetsTransactionRepo.findByUuid(resource.getRevertedAssetTxUuid())
                    .orElseThrow(() -> new EntityNotFoundException(messageTranslator.translate(
                            "{0} \"{1}\" not found",
                            "revertedAssetTx.uuid",
                            resource.getRevertedAssetTxUuid())));
            revertedAssetTx.setReversalAssetTx(entity);
            entity.setRevertedAssetTx(revertedAssetTx);
            if (resource.getAssetValue().negate().compareTo(revertedAssetTx.getAssetValue()) != 0) {
                throw new ValidationException(
                        messageTranslator.translate(
                                "given assetValue {0,number,#0.00} must be the negative value of the reverted asset transaction: {1,number,#0.00}",
                                resource.getAssetValue(), revertedAssetTx.getAssetValue()));
            }

            if (revertedAssetTx.getTransactionType() == HsOfficeCoopAssetsTransactionType.TRANSFER) {
                final var adoptionAssetTx = revertedAssetTx.getAdoptionAssetTx();
                final var adoptionReversalAssetTx = HsOfficeCoopAssetsTransactionEntity.builder()
                        .transactionType(REVERSAL)
                        .membership(adoptionAssetTx.getMembership())
                        .revertedAssetTx(adoptionAssetTx)
                        .assetValue(adoptionAssetTx.getAssetValue().negate())
                        .comment(resource.getComment())
                        .reference(resource.getReference())
                        .valueDate(resource.getValueDate())
                        .build();
                adoptionAssetTx.setReversalAssetTx(adoptionReversalAssetTx);
                adoptionReversalAssetTx.setRevertedAssetTx(adoptionAssetTx);
            }
        }

        if (resource.getTransactionType() == HsOfficeCoopAssetsTransactionTypeResource.TRANSFER) {
            final var adoptingMembership = determineAdoptingMembership(resource);
            if ( entity.getMembership() == adoptingMembership) {
                throw new ValidationException(messageTranslator.translate(
                        "transferring and adopting membership must be different, but both are {0}",
                        adoptingMembership.getTaggedMemberNumber()));
            }
            final var adoptingAssetTx = createAdoptingAssetTx(entity, adoptingMembership);
            entity.setAdoptionAssetTx(adoptingAssetTx);
        }
    };

    private HsOfficeMembershipEntity determineAdoptingMembership(final HsOfficeCoopAssetsTransactionInsertResource resource) {
        final var adoptingMembershipUuid = resource.getAdoptingMembershipUuid();
        final var adoptingMembershipMemberNumber = resource.getAdoptingMembershipMemberNumber();
        if (adoptingMembershipUuid != null && adoptingMembershipMemberNumber != null) {
            // @formatter:off
            final var message =  messageTranslator.translate(
                    resource.getTransactionType() == HsOfficeCoopAssetsTransactionTypeResource.TRANSFER
                            ? "either {0} or {1} must be given, not both"
                            : "neither {0} nor {1} must be given for transactionType={2}",
                            "adoptingMembership.uuid",
                            "adoptingMembership.memberNumber",
                            resource.getTransactionType());
            // @formatter:on
            throw new ValidationException(message);
        }

        if (adoptingMembershipUuid != null) {
            final var adoptingMembership = membershipRepo.findByUuid(adoptingMembershipUuid);
            return adoptingMembership.orElseThrow(() ->
                    new ValidationException(messageTranslator.translate(
                            "{0} \"{1}\" not found or not accessible",
                            "adoptingMembership.uuid",
                            adoptingMembershipUuid)));
        }

        if (adoptingMembershipMemberNumber != null) {
            final var adoptingMemberNumber = Integer.valueOf(adoptingMembershipMemberNumber.substring("M-".length()));
            final var adoptingMembership = membershipRepo.findMembershipByMemberNumber(adoptingMemberNumber);
            return adoptingMembership.orElseThrow( () ->
                    new ValidationException(
                            messageTranslator.translate(
                                    "{0} \"{1}\" not found or not accessible",
                                    "adoptingMembership.memberNumber",
                                    adoptingMembershipMemberNumber)));
        }

        throw new ValidationException(
                messageTranslator.translate(
                        "either {0} or {1} must be given for transactionType={2}",
                        "adoptingMembership.uuid",
                        "adoptingMembership.memberNumber",
                        resource.getTransactionType()
                        ));
    }

    private HsOfficeCoopAssetsTransactionEntity createAdoptingAssetTx(
            final HsOfficeCoopAssetsTransactionEntity transferAssetTxEntity,
            final HsOfficeMembershipEntity adoptingMembership) {
        return HsOfficeCoopAssetsTransactionEntity.builder()
                .membership(adoptingMembership)
                .transactionType(HsOfficeCoopAssetsTransactionType.ADOPTION)
                .transferAssetTx(transferAssetTxEntity)
                .assetValue(transferAssetTxEntity.getAssetValue().negate())
                .comment(transferAssetTxEntity.getComment())
                .reference(transferAssetTxEntity.getReference())
                .valueDate(transferAssetTxEntity.getValueDate())
                .build();
    }
}
