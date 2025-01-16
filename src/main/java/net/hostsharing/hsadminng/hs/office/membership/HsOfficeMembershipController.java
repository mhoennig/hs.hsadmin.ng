package net.hostsharing.hsadminng.hs.office.membership;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeMembershipsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeMembershipInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeMembershipPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeMembershipResource;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRbacEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRealRepository;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import static net.hostsharing.hsadminng.errors.Validate.validate;
import static net.hostsharing.hsadminng.repr.TaggedNumber.cropTag;

@RestController
public class HsOfficeMembershipController implements HsOfficeMembershipsApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private HsOfficePartnerRealRepository partnerRepo;

    @Autowired
    private HsOfficeMembershipRepository membershipRepo;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.membership.api.getListOfMemberships")
    public ResponseEntity<List<HsOfficeMembershipResource>> getListOfMemberships(
            final String currentSubject,
            final String assumedRoles,
            final UUID partnerUuid,
            final String partnerNumber) {
        context.define(currentSubject, assumedRoles);

        validate("partnerUuid, partnerNumber").atMaxOne(partnerUuid, partnerNumber);

        final var entities = partnerNumber != null
                ? membershipRepo.findMembershipsByPartnerNumber(
                        cropTag(HsOfficePartnerRbacEntity.PARTNER_NUMBER_TAG, partnerNumber))
                : partnerUuid != null
                    ? membershipRepo.findMembershipsByPartnerUuid(partnerUuid)
                    : membershipRepo.findAll();

        final var resources = mapper.mapList(
                entities, HsOfficeMembershipResource.class,
                SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    @Timed("app.office.membership.api.postNewMembership")
    public ResponseEntity<HsOfficeMembershipResource> postNewMembership(
            final String currentSubject,
            final String assumedRoles,
            final HsOfficeMembershipInsertResource body) {

        context.define(currentSubject, assumedRoles);

        final var entityToSave = mapper.map(body, HsOfficeMembershipEntity.class, SEPA_MANDATE_RESOURCE_TO_ENTITY_POSTMAPPER);

        final var saved = membershipRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/memberships/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(
                saved, HsOfficeMembershipResource.class,
                SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.membership.api.getSingleMembershipByUuid")
    public ResponseEntity<HsOfficeMembershipResource> getSingleMembershipByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID membershipUuid) {

        context.define(currentSubject, assumedRoles);

        final var result = membershipRepo.findByUuid(membershipUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(
                result.get(), HsOfficeMembershipResource.class,
                SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER));
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.membership.api.getSingleMembershipByMembershipNumber")
    public ResponseEntity<HsOfficeMembershipResource> getSingleMembershipByMembershipNumber(
            final String currentSubject,
            final String assumedRoles,
            final Integer membershipNumber) {

        context.define(currentSubject, assumedRoles);

        final var result = membershipRepo.findMembershipByMemberNumber(membershipNumber);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(
                result.get(), HsOfficeMembershipResource.class,
                SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER));
    }

    @Override
    @Transactional
    @Timed("app.office.membership.api.deleteMembershipByUuid")
    public ResponseEntity<Void> deleteMembershipByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID membershipUuid) {
        context.define(currentSubject, assumedRoles);

        final var result = membershipRepo.deleteByUuid(membershipUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    @Timed("app.office.membership.api.patchMembership")
    public ResponseEntity<HsOfficeMembershipResource> patchMembership(
            final String currentSubject,
            final String assumedRoles,
            final UUID membershipUuid,
            final HsOfficeMembershipPatchResource body) {

        context.define(currentSubject, assumedRoles);

        final var current = membershipRepo.findByUuid(membershipUuid).orElseThrow();

        new HsOfficeMembershipEntityPatcher(mapper, current).apply(body);

        final var saved = membershipRepo.save(current);
        final var mapped = mapper.map(saved, HsOfficeMembershipResource.class, SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(mapped);
    }

    final BiConsumer<HsOfficeMembershipEntity, HsOfficeMembershipResource> SEPA_MANDATE_ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setMemberNumber(entity.getTaggedMemberNumber());
        resource.setValidFrom(entity.getValidity().lower());
        if (entity.getValidity().hasUpperBound()) {
            resource.setValidTo(entity.getValidity().upper().minusDays(1));
        }
        resource.getPartner().setPartnerNumber(entity.getPartner().getTaggedPartnerNumber()); // TODO.refa: use partner mapper?
    };

    final BiConsumer<HsOfficeMembershipInsertResource, HsOfficeMembershipEntity> SEPA_MANDATE_RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        entity.setPartner(partnerRepo.findByUuid(resource.getPartnerUuid())
                .orElseThrow(() -> new EntityNotFoundException(
                        "ERROR: [400] partnerUuid %s not found".formatted(resource.getPartnerUuid()))));
    };
}
