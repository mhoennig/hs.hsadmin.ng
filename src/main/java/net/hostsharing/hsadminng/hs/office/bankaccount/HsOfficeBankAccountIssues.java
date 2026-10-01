package net.hostsharing.hsadminng.hs.office.bankaccount;

import lombok.val;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankAccountBankDirectoryMismatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankAccountIssuesResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankAccountResource;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankAccountBankDirectoryMismatchReasonResource.BIC_MISMATCH;
import static net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankAccountBankDirectoryMismatchReasonResource.UNKNOWN_BANK_CODE;

/** Bank accounts entered before the bank directory was complete, or changed by a later import, might not match it. */
@Component
public class HsOfficeBankAccountIssues {

    private final HsOfficeBankRepository bankRepo;

    public HsOfficeBankAccountIssues(final HsOfficeBankRepository bankRepo) {
        this.bankRepo = bankRepo;
    }

    public void addTo(final HsOfficeBankAccountResource bankAccount) {
        addTo(bankAccount, bankCode -> bankRepo.findByCountryCodeAndBankCode("DE", bankCode));
    }

    public void addTo(final Collection<HsOfficeBankAccountResource> bankAccounts) {
        val germanBankDirectory = germanBankDirectory();
        bankAccounts.forEach(bankAccount -> addTo(bankAccount, germanBankDirectory::lookup));
    }

    /** Loads the German banks just once for checking many bank accounts. */
    public GermanBankDirectory germanBankDirectory() {
        return new GermanBankDirectory(bankRepo.findAllByCountryCode("DE").stream()
                .collect(Collectors.toMap(HsOfficeBankEntity::getBankCode, Function.identity())));
    }

    public record GermanBankDirectory(Map<String, HsOfficeBankEntity> banks) {

        public boolean hasIssues(final String iban, final String bic) {
            return mismatchOf(iban, bic, this::lookup).isPresent();
        }

        private Optional<HsOfficeBankEntity> lookup(final String bankCode) {
            return Optional.ofNullable(banks.get(bankCode));
        }
    }

    private static void addTo(
            final HsOfficeBankAccountResource bankAccount,
            final Function<String, Optional<HsOfficeBankEntity>> germanBankLookup) {
        if (bankAccount == null) {
            return;
        }
        mismatchOf(bankAccount.getIban(), bankAccount.getBic(), germanBankLookup).ifPresent(mismatch -> {
            val issues = new HsOfficeBankAccountIssuesResource();
            issues.setBankDirectoryMismatch(mismatch);
            bankAccount.setIssues(issues);
        });
    }

    private static Optional<HsOfficeBankAccountBankDirectoryMismatchResource> mismatchOf(
            final String iban,
            final String bic,
            final Function<String, Optional<HsOfficeBankEntity>> germanBankLookup) {
        val bankCode = iban == null ? Optional.<String>empty() : HsOfficeBankEntity.germanBankCodeOf(iban);
        if (bankCode.isEmpty()) {
            return Optional.empty();
        }
        val bank = germanBankLookup.apply(bankCode.get()).orElse(null);
        if (HsOfficeBankEntity.matches(bank, bic)) {
            return Optional.empty();
        }
        val mismatch = new HsOfficeBankAccountBankDirectoryMismatchResource();
        mismatch.setReason(bank == null ? UNKNOWN_BANK_CODE : BIC_MISMATCH);
        mismatch.setBankCode(bankCode.get());
        mismatch.setExpectedBic(bank == null ? null : bank.getBic());
        return Optional.of(mismatch);
    }
}
