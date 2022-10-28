package net.hostsharing.hsadminng.hs.office.bankaccount;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeBankAccountsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankAccountInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankAccountResource;
import net.hostsharing.hsadminng.mapper.Mapper;
import org.iban4j.BicUtil;
import org.iban4j.IbanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController

public class HsOfficeBankAccountController implements HsOfficeBankAccountsApi {

    @Autowired
    private Context context;

    @Autowired
    private Mapper mapper;

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

        final var resources = mapper.mapList(entities, HsOfficeBankAccountResource.class);
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

        final var entityToSave = mapper.map(body, HsOfficeBankAccountEntity.class);


        final var saved = bankAccountRepo.save(entityToSave);
//        em.persist(entityToSave);
//        final var saved = entityToSave;
//        em.flush();

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/bankaccounts/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsOfficeBankAccountResource.class);
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
        return ResponseEntity.ok(mapper.map(result.get(), HsOfficeBankAccountResource.class));
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
