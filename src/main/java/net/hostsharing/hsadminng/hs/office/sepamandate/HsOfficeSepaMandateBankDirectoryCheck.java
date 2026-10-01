package net.hostsharing.hsadminng.hs.office.sepamandate;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankDirectoryController;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankDirectoryImporter;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountIssues;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Tells after a bank-directory import and to the monitoring whether bank accounts which still get debited need a correction. */
@Slf4j
@Component
public class HsOfficeSepaMandateBankDirectoryCheck implements HsOfficeBankDirectoryController.SepaMandateIssues {

    private final HsOfficeSepaMandateRealRepository sepaMandateRealRepo;
    private final HsOfficeBankAccountIssues bankAccountIssues;

    public HsOfficeSepaMandateBankDirectoryCheck(
            final HsOfficeSepaMandateRealRepository sepaMandateRealRepo,
            final HsOfficeBankAccountIssues bankAccountIssues) {
        this.sepaMandateRealRepo = sepaMandateRealRepo;
        this.bankAccountIssues = bankAccountIssues;
    }

    // the log gets no account data, the endpoint shows it only to those who may see the SEPA mandates
    @EventListener
    public void logNumberOfMismatchingSepaMandates(final HsOfficeBankDirectoryImporter.Imported imported) {
        val mismatches = countValidSepaMandatesWithIssues();
        if (mismatches == 0) {
            log.info("the bank accounts of all valid SEPA mandates match the bank directory {}", imported.sourceUrl());
        } else {
            log.warn("{} valid SEPA mandates have a bank account which does not match the bank directory {}, "
                    + "GET /api/hs/office/sepamandates?issues=true lists them", mismatches, imported.sourceUrl());
        }
    }

    @Override
    public int countValidSepaMandatesWithIssues() {
        val germanBankDirectory = bankAccountIssues.germanBankDirectory();
        return Math.toIntExact(sepaMandateRealRepo.findBankAccountsOfValidSepaMandates().stream()
                .filter(bankAccount -> germanBankDirectory.hasIssues(bankAccount.getIban(), bankAccount.getBic()))
                .count());
    }
}
