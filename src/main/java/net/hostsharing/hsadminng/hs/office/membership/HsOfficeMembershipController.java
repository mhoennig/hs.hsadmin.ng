package net.hostsharing.hsadminng.hs.office.membership;

import com.vladmihalcea.hibernate.type.range.Range;
import net.hostsharing.hsadminng.mapper.Mapper;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeMembershipsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeMembershipInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeMembershipPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeMembershipResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.persistence.EntityManager;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

import static net.hostsharing.hsadminng.mapper.Mapper.map;

@RestController

public class HsOfficeMembershipController implements HsOfficeMembershipsApi {

    @Autowired
    private Context context;

    @Autowired
    private HsOfficeMembershipRepository membershipRepo;

    @Autowired
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficeMembershipResource>> listMemberships(
            final String currentUser,
            final String assumedRoles,
            UUID partnerUuid,
            Integer memberNumber) {
        context.define(currentUser, assumedRoles);

        final var entities =
                membershipRepo.findMembershipsByOptionalPartnerUuidAndOptionalMemberNumber(partnerUuid, memberNumber);

        final var resources = Mapper.mapList(entities, HsOfficeMembershipResource.class,
                SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeMembershipResource> addMembership(
            final String currentUser,
            final String assumedRoles,
            @Valid final HsOfficeMembershipInsertResource body) {

        context.define(currentUser, assumedRoles);

        final var entityToSave = map(body, HsOfficeMembershipEntity.class, SEPA_MANDATE_RESOURCE_TO_ENTITY_POSTMAPPER);
        entityToSave.setUuid(UUID.randomUUID());

        final var saved = membershipRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/Memberships/{id}")
                        .buildAndExpand(entityToSave.getUuid())
                        .toUri();
        final var mapped = map(saved, HsOfficeMembershipResource.class,
                SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<HsOfficeMembershipResource> getMembershipByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID membershipUuid) {

        context.define(currentUser, assumedRoles);

        final var result = membershipRepo.findByUuid(membershipUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(map(result.get(), HsOfficeMembershipResource.class,
                SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteMembershipByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID membershipUuid) {
        context.define(currentUser, assumedRoles);

        final var result = membershipRepo.deleteByUuid(membershipUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeMembershipResource> patchMembership(
            final String currentUser,
            final String assumedRoles,
            final UUID membershipUuid,
            final HsOfficeMembershipPatchResource body) {

        context.define(currentUser, assumedRoles);

        final var current = membershipRepo.findByUuid(membershipUuid).orElseThrow();

        current.setValidity(toPostgresDateRange(current.getValidity().lower(), body.getValidTo()));
//        current.setReasonForTermination(HsOfficeReasonForTermination.valueOf(body.getReasonForTermination().name()));
        current.setReasonForTermination(
                Optional.ofNullable(body.getReasonForTermination()).map(Enum::name).map(HsOfficeReasonForTermination::valueOf).orElse(current.getReasonForTermination())
        );

        final var saved = membershipRepo.save(current);
        final var mapped = map(saved, HsOfficeMembershipResource.class, SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(mapped);
    }

    private static Range<LocalDate> toPostgresDateRange(
            final LocalDate validFrom,
            final LocalDate validTo) {
        return validTo != null
                ? Range.closedOpen(validFrom, validTo.plusDays(1))
                : Range.closedInfinite(validFrom);
    }

    final BiConsumer<HsOfficeMembershipEntity, HsOfficeMembershipResource> SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setValidFrom(entity.getValidity().lower());
        if (entity.getValidity().hasUpperBound()) {
            resource.setValidTo(entity.getValidity().upper().minusDays(1));
        }
    };

    final BiConsumer<HsOfficeMembershipInsertResource, HsOfficeMembershipEntity> SEPA_MANDATE_RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        entity.setValidity(toPostgresDateRange(resource.getValidFrom(), resource.getValidTo()));
    };
}
