package net.hostsharing.hsadminng.hs.office.debitor;

import net.hostsharing.hsadminng.Mapper;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeDebitorsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.*;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntityPatcher;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import static net.hostsharing.hsadminng.Mapper.map;

@RestController

public class HsOfficeDebitorController implements HsOfficeDebitorsApi {

    @Autowired
    private Context context;

    @Autowired
    private HsOfficeDebitorRepository debitorRepo;

    @Autowired
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficeDebitorResource>> listDebitors(
            final String currentUser,
            final String assumedRoles,
            final String name,
            final Integer debitorNumber) {
        context.define(currentUser, assumedRoles);

        final var entities = debitorNumber != null
                ? debitorRepo.findDebitorByDebitorNumber(debitorNumber)
                : debitorRepo.findDebitorByOptionalNameLike(name);

        final var resources = Mapper.mapList(entities, HsOfficeDebitorResource.class,
                DEBITOR_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeDebitorResource> addDebitor(
            final String currentUser,
            final String assumedRoles,
            final HsOfficeDebitorInsertResource body) {

        context.define(currentUser, assumedRoles);

        final var entityToSave = map(body, HsOfficeDebitorEntity.class, DEBITOR_RESOURCE_TO_ENTITY_POSTMAPPER);
        entityToSave.setUuid(UUID.randomUUID());

        final var saved = debitorRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/debitors/{id}")
                        .buildAndExpand(entityToSave.getUuid())
                        .toUri();
        final var mapped = map(saved, HsOfficeDebitorResource.class,
                DEBITOR_ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<HsOfficeDebitorResource> getDebitorByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID debitorUuid) {

        context.define(currentUser, assumedRoles);

        final var result = debitorRepo.findByUuid(debitorUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(map(result.get(), HsOfficeDebitorResource.class, DEBITOR_ENTITY_TO_RESOURCE_POSTMAPPER));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteDebitorByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID debitorUuid) {
        context.define(currentUser, assumedRoles);

        final var result = debitorRepo.deleteByUuid(debitorUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeDebitorResource> patchDebitor(
            final String currentUser,
            final String assumedRoles,
            final UUID debitorUuid,
            final HsOfficeDebitorPatchResource body) {

        context.define(currentUser, assumedRoles);

        final var current = debitorRepo.findByUuid(debitorUuid).orElseThrow();

        new HsOfficeDebitorEntityPatcher(em, current).apply(body);

        final var saved = debitorRepo.save(current);
        final var mapped = map(saved, HsOfficeDebitorResource.class);
        return ResponseEntity.ok(mapped);
    }


    final BiConsumer<HsOfficeDebitorEntity, HsOfficeDebitorResource> DEBITOR_ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setPartner(map(entity.getPartner(), HsOfficePartnerResource.class));
        resource.setBillingContact(map(entity.getBillingContact(), HsOfficeContactResource.class));
        if ( entity.getRefundBankAccount() != null ) {
            resource.setRefundBankAccount(map(entity.getRefundBankAccount(), HsOfficeBankAccountResource.class));
        }
    };

    final BiConsumer<HsOfficeDebitorInsertResource, HsOfficeDebitorEntity> DEBITOR_RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        entity.setPartner(em.getReference(HsOfficePartnerEntity.class, resource.getPartnerUuid()));
        entity.setBillingContact(em.getReference(HsOfficeContactEntity.class, resource.getBillingContactUuid()));
        if ( resource.getRefundBankAccountUuid() != null ) {
            entity.setRefundBankAccount(em.getReference(HsOfficeBankAccountEntity.class, resource.getRefundBankAccountUuid()));
        }
    };
}
