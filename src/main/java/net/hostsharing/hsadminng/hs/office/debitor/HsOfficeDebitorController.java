package net.hostsharing.hsadminng.hs.office.debitor;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountRepository;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealRepository;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeDebitorsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeDebitorInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeDebitorPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeDebitorResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealRepository;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import org.apache.commons.lang3.Validate;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ValidationException;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import static net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType.DEBITOR;
import static net.hostsharing.hsadminng.hs.validation.UuidResolver.resolve;
import static net.hostsharing.hsadminng.repr.TaggedNumber.cropTag;

@RestController
@SecurityRequirement(name = "casTicket")
public class HsOfficeDebitorController implements HsOfficeDebitorsApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private HsOfficeDebitorRepository debitorRepo;

    @Autowired
    private HsOfficeRelationRealRepository realRelRepo;

    @Autowired
    private HsOfficePersonRealRepository realPersonRepo;

    @Autowired
    private HsOfficeContactRealRepository realContactRepo;

    @Autowired
    private HsOfficeBankAccountRepository bankAccountRepo;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.debitors.api.getListOfDebitors")
    public ResponseEntity<List<HsOfficeDebitorResource>> getListOfDebitors(
            final String assumedRoles,
            final String name,
            final UUID partnerUuid,
            final String partnerNumber) {
        context.assumeRoles(assumedRoles);

        final var entities = partnerNumber != null
                ? debitorRepo.findDebitorsByPartnerNumber(cropTag("P-", partnerNumber))
                : partnerUuid != null
                ? debitorRepo.findDebitorsByPartnerUuid(partnerUuid)
                : debitorRepo.findDebitorsByOptionalNameLike(name);

        final var resources = mapper.mapList(entities, HsOfficeDebitorResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    @Timed("app.office.debitors.api.postNewDebitor")
    public ResponseEntity<HsOfficeDebitorResource> postNewDebitor(
            String assumedRoles,
            HsOfficeDebitorInsertResource body) {

        context.assumeRoles(assumedRoles);

        Validate.isTrue(
                body.getDebitorRel() == null || body.getDebitorRelUuid() == null,
                "ERROR: [400] exactly one of debitorRel and debitorRelUuid must be supplied, but found both");
        Validate.isTrue(
                body.getDebitorRel() != null || body.getDebitorRelUuid() != null,
                "ERROR: [400] exactly one of debitorRel and debitorRelUuid must be supplied, but found none");
        Validate.isTrue(
                body.getDebitorRel() == null || body.getDebitorRel().getMark() == null,
                "ERROR: [400] debitorRel.mark must be null");

        final var entityToSave = mapper.map(body, HsOfficeDebitorEntity.class, RESOURCE_TO_ENTITY_POSTMAPPER);

        final var savedEntity = debitorRepo.save(entityToSave).reload(em);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/debitors/{id}")
                        .buildAndExpand(savedEntity.getUuid())
                        .toUri();
        final var mapped = mapper.map(savedEntity, HsOfficeDebitorResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.debitors.api.getSingleDebitorByUuid")
    public ResponseEntity<HsOfficeDebitorResource> getSingleDebitorByUuid(
            final String assumedRoles,
            final UUID debitorUuid) {

        context.assumeRoles(assumedRoles);

        final var result = debitorRepo.findByUuid(debitorUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result.get(), HsOfficeDebitorResource.class, ENTITY_TO_RESOURCE_POSTMAPPER));
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.debitors.api.getSingleDebitorByDebitorNumber")
    public ResponseEntity<HsOfficeDebitorResource> getSingleDebitorByDebitorNumber(
            final String assumedRoles,
            final Integer debitorNumber) {

        context.assumeRoles(assumedRoles);

        final var result = debitorRepo.findDebitorByDebitorNumber(debitorNumber);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result.get(), HsOfficeDebitorResource.class, ENTITY_TO_RESOURCE_POSTMAPPER));
    }

    @Override
    @Transactional
    @Timed("app.office.debitors.api.deleteDebitorByUuid")
    public ResponseEntity<Void> deleteDebitorByUuid(
            final String assumedRoles,
            final UUID debitorUuid) {
        context.assumeRoles(assumedRoles);

        final var result = debitorRepo.deleteByUuid(debitorUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    @Timed("app.office.debitors.api.patchDebitor")
    public ResponseEntity<HsOfficeDebitorResource> patchDebitor(
            final String assumedRoles,
            final UUID debitorUuid,
            final HsOfficeDebitorPatchResource body) {

        context.assumeRoles(assumedRoles);

        final var current = debitorRepo.findByUuid(debitorUuid).orElseThrow().reload(em);

        new HsOfficeDebitorEntityPatcher(em, current).apply(body);

        final var saved = debitorRepo.save(current);
        Hibernate.initialize(saved);
        final var mapped = mapper.map(saved, HsOfficeDebitorResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(mapped);
    }

    final BiConsumer<HsOfficeDebitorInsertResource, HsOfficeDebitorEntity> RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        if (resource.getDebitorRel() != null) {
            final var debitorRel = realRelRepo.save(HsOfficeRelationRealEntity.builder()
                    .type(DEBITOR)
                    .anchor(resolve(
                            "debitorRel.anchor.uuid", resource.getDebitorRel().getAnchorUuid(), realPersonRepo::findByUuid))
                    .holder(resolve(
                            "debitorRel.holder.uuid", resource.getDebitorRel().getHolderUuid(), realPersonRepo::findByUuid))
                    .contact(resolve(
                            "debitorRel.contact.uuid", resource.getDebitorRel().getContactUuid(), realContactRepo::findByUuid))
                    .build());
            entity.setDebitorRel(debitorRel);
        } else {
            final var debitorRelOptional = realRelRepo.findByUuid(resource.getDebitorRelUuid());
            debitorRelOptional.ifPresentOrElse(
                    debitorRel -> {
                        entity.setDebitorRel(realRelRepo.save(debitorRel));
                        },
                    () -> {
                        throw new ValidationException(
                                "Unable to find debitorRel.uuid: " + resource.getDebitorRelUuid());
                    });
        }

        if (resource.getRefundBankAccountUuid() != null) {
            entity.setRefundBankAccount(resolve(
                    "refundBankAccount.uuid", resource.getRefundBankAccountUuid(), bankAccountRepo::findByUuid));
        }

    };

    final BiConsumer<HsOfficeDebitorEntity, HsOfficeDebitorResource> ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setDebitorNumber(entity.getTaggedDebitorNumber());
        resource.getPartner().setPartnerNumber(entity.getPartner().getTaggedPartnerNumber());
    };
}
