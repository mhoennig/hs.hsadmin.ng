package net.hostsharing.hsadminng.hs.office.bankaccount;

import net.hostsharing.hsadminng.Mapper;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeBankAccountsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankAccountInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankAccountResource;
import org.iban4j.BicUtil;
import org.iban4j.IbanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.Mapper.map;

@RestController

public class HsOfficeBankAccountController implements HsOfficeBankAccountsApi {

    @Autowired
    private Context context;

    @Autowired
    private HsOfficeBankAccountRepository bankAccountRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficeBankAccountResource>> listBankAccounts(
            final String currentUser,
            final String assumedRoles,
            final String holder) {
        context.define(currentUser, assumedRoles);

        final var entities = bankAccountRepo.findByOptionalHolderLike(holder);

        final var resources = Mapper.mapList(entities, HsOfficeBankAccountResource.class);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeBankAccountResource> addBankAccount(
            final String currentUser,
            final String assumedRoles,
            final HsOfficeBankAccountInsertResource body) {

        context.define(currentUser, assumedRoles);

        IbanUtil.validate(body.getIban());
        BicUtil.validate(body.getBic());

        final var entityToSave = map(body, HsOfficeBankAccountEntity.class);
        entityToSave.setUuid(UUID.randomUUID());

        final var saved = bankAccountRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/BankAccounts/{id}")
                        .buildAndExpand(entityToSave.getUuid())
                        .toUri();
        final var mapped = map(saved, HsOfficeBankAccountResource.class);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<HsOfficeBankAccountResource> getBankAccountByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID BankAccountUuid) {

        context.define(currentUser, assumedRoles);

        final var result = bankAccountRepo.findByUuid(BankAccountUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(map(result.get(), HsOfficeBankAccountResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteBankAccountByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID BankAccountUuid) {
        context.define(currentUser, assumedRoles);

        final var result = bankAccountRepo.deleteByUuid(BankAccountUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
