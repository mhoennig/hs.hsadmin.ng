package net.hostsharing.hsadminng.hs.office.coopshares;

import net.hostsharing.hsadminng.Mapper;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeCoopSharesApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopSharesTransactionInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeCoopSharesTransactionResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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

import static net.hostsharing.hsadminng.Mapper.map;

@RestController

public class HsOfficeCoopSharesTransactionController implements HsOfficeCoopSharesApi {

    @Autowired
    private Context context;

    @Autowired
    private HsOfficeCoopSharesTransactionRepository coopSharesTransactionRepo;

    @Autowired
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficeCoopSharesTransactionResource>> listCoopShares(
            final String currentUser,
            final String assumedRoles,
            final UUID membershipUuid,
            final @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromValueDate,
            final @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toValueDate) {
        context.define(currentUser, assumedRoles);


        final var entities = coopSharesTransactionRepo.findCoopSharesTransactionByOptionalMembershipUuidAndDateRange(
                membershipUuid,
                fromValueDate,
                toValueDate);

        final var resources = Mapper.mapList(entities, HsOfficeCoopSharesTransactionResource.class,
                COOP_SHARES_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeCoopSharesTransactionResource> addCoopSharesTransaction(
            final String currentUser,
            final String assumedRoles,
            @Valid final HsOfficeCoopSharesTransactionInsertResource body) {

        context.define(currentUser, assumedRoles);

        final var entityToSave = map(
                body,
                HsOfficeCoopSharesTransactionEntity.class,
                COOP_SHARES_RESOURCE_TO_ENTITY_POSTMAPPER);
        entityToSave.setUuid(UUID.randomUUID());

        final var saved = coopSharesTransactionRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/CoopSharesTransactions/{id}")
                        .buildAndExpand(entityToSave.getUuid())
                        .toUri();
        final var mapped = map(saved, HsOfficeCoopSharesTransactionResource.class,
                COOP_SHARES_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(mapped);
    }

    final BiConsumer<HsOfficeCoopSharesTransactionEntity, HsOfficeCoopSharesTransactionResource> COOP_SHARES_ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        //        resource.setValidFrom(entity.getValidity().lower());
        //        if (entity.getValidity().hasUpperBound()) {
        //            resource.setValidTo(entity.getValidity().upper().minusDays(1));
        //        }
    };

    final BiConsumer<HsOfficeCoopSharesTransactionInsertResource, HsOfficeCoopSharesTransactionEntity> COOP_SHARES_RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        //        entity.setValidity(toPostgresDateRange(resource.getValidFrom(), resource.getValidTo()));
    };
}
