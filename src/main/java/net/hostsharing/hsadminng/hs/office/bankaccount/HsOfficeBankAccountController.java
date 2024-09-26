package net.hostsharing.hsadminng.hs.office.bankaccount;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeBankAccountsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankAccountInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankAccountResource;
import net.hostsharing.hsadminng.mapper.StandardMapper;
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
    private StandardMapper mapper;

    @Autowired
    private HsOfficeBankAccountRepository bankAccountRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsOfficeBankAccountResource>> listBankAccounts(
            final String currentSubject,
            final String assumedRoles,
            final String holder) {
        context.define(currentSubject, assumedRoles);

        final var entities = bankAccountRepo.findByOptionalHolderLike(holder);

        final var resources = mapper.mapList(entities, HsOfficeBankAccountResource.class);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsOfficeBankAccountResource> addBankAccount(
            final String currentSubject,
            final String assumedRoles,
            final HsOfficeBankAccountInsertResource body) {

        context.define(currentSubject, assumedRoles);

        IbanUtil.validate(body.getIban());
        BicUtil.validate(body.getBic());

        final var entityToSave = mapper.map(body, HsOfficeBankAccountEntity.class);


        final var saved = bankAccountRepo.save(entityToSave);

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
            final String currentSubject,
            final String assumedRoles,
            final UUID bankAccountUuid) {

        context.define(currentSubject, assumedRoles);

        final var result = bankAccountRepo.findByUuid(bankAccountUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result.get(), HsOfficeBankAccountResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteBankAccountByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID BankAccountUuid) {
        context.define(currentSubject, assumedRoles);

        final var result = bankAccountRepo.deleteByUuid(BankAccountUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
