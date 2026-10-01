package net.hostsharing.hsadminng.hs.office.bankaccount;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.errors.ForbiddenException;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeBankAccountsApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankAccountInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankAccountResource;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import jakarta.validation.ValidationException;
import lombok.val;
import org.iban4j.BicFormatException;
import org.iban4j.BicUtil;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
public class HsOfficeBankAccountController implements HsOfficeBankAccountsApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private HsOfficeBankAccountRepository bankAccountRepo;

    @Autowired
    private HsOfficeBankRepository bankRepo;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.bankAccounts.api.patchDebitor")
    public ResponseEntity<List<HsOfficeBankAccountResource>> getListOfBankAccounts(
            final String assumedRoles,
            final String holder) {
        context.assumeRoles(assumedRoles);

        final var entities = bankAccountRepo.findByOptionalHolderLike(holder);

        final var resources = mapper.mapList(entities, HsOfficeBankAccountResource.class);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    @Timed("app.office.bankAccounts.api.postNewBankAccount")
    public ResponseEntity<HsOfficeBankAccountResource> postNewBankAccount(
            final String assumedRoles,
            final HsOfficeBankAccountInsertResource body) {

        context.assumeRoles(assumedRoles);

        val bank = validateOrCompleteBic(body, Iban.valueOf(body.getIban()));

        final var entityToSave = mapper.map(body, HsOfficeBankAccountEntity.class);

        final var saved = bankAccountRepo.save(entityToSave);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/office/bankaccounts/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsOfficeBankAccountResource.class);
        // the entity reads bankName through a formula, which Hibernate only evaluates when loading
        mapped.setBankName(bank == null ? null : bank.getName());
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.office.bankAccounts.api.getSingleBankAccountByUuid")
    public ResponseEntity<HsOfficeBankAccountResource> getSingleBankAccountByUuid(
            final String assumedRoles,
            final UUID bankAccountUuid) {

        context.assumeRoles(assumedRoles);

        final var result = bankAccountRepo.findByUuid(bankAccountUuid);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result.get(), HsOfficeBankAccountResource.class));
    }

    @Override
    @Transactional
    @Timed("app.office.bankAccounts.api.deleteBankAccountByUuid")
    public ResponseEntity<Void> deleteBankAccountByUuid(
            final String assumedRoles,
            final UUID BankAccountUuid) {
        context.assumeRoles(assumedRoles);

        final var result = bankAccountRepo.deleteByUuid(BankAccountUuid);
        if (result == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    // The bank directory determines the BIC; it is meant to be complete for German IBANs, for other countries only manually maintained.
    private HsOfficeBankEntity validateOrCompleteBic(final HsOfficeBankAccountInsertResource body, final Iban iban) {
        val bank = bankOfIban(body, iban);
        if (body.getBic() == null) {
            body.setBic(bicFromDirectory(bank, body.getIban()));
        } else {
            validateGivenBic(body, bank);
        }
        return bank;
    }

    private HsOfficeBankEntity bankOfIban(final HsOfficeBankAccountInsertResource body, final Iban iban) {
        val bank = bankRepo.findByCountryCodeAndBankCode(iban.getCountryCode().name(), iban.getBankCode())
                .orElse(null);
        if (bank == null && iban.getCountryCode() == CountryCode.DE) {
            if (!Boolean.TRUE.equals(body.getOverrideBankDirectory())) {
                throw new ValidationException("IBAN " + body.getIban() + " has unknown bank code " + iban.getBankCode());
            }
            requireGlobalAdminRoleForOverrideBankDirectory();
        }
        return bank;
    }

    private String bicFromDirectory(final HsOfficeBankEntity bank, final String iban) {
        if (bank == null || bank.getBic() == null) {
            throw new ValidationException("bic is required for IBAN " + iban);
        }
        // the directory rows seeded from existing bank accounts or maintained manually are not guaranteed to be valid
        try {
            BicUtil.validate(bank.getBic());
        } catch (final BicFormatException exc) {
            throw new ValidationException("bank directory has invalid BIC " + bank.getBic()
                    + " for bank code " + bank.getBankCode() + ", bic is required for IBAN " + iban);
        }
        return bank.getBic();
    }

    private void validateGivenBic(final HsOfficeBankAccountInsertResource body, final HsOfficeBankEntity bank) {
        BicUtil.validate(body.getBic());
        if (bank == null || bank.getBic() == null || bank.hasBic(body.getBic())) {
            return;
        }
        if (!Boolean.TRUE.equals(body.getOverrideBankDirectory())) {
            throw new ValidationException("BIC " + body.getBic() + " does not match bank code " + bank.getBankCode()
                    + " of IBAN " + body.getIban() + ", expected " + bank.getBic());
        }
        requireGlobalAdminRoleForOverrideBankDirectory();
    }

    // context.requireGlobalAdmin(...) would redefine the context and thus drop the assumed roles.
    private void requireGlobalAdminRoleForOverrideBankDirectory() {
        if (!context.hasGlobalAdminRole()) {
            throw new ForbiddenException("overrideBankDirectory requires global admin");
        }
    }
}
