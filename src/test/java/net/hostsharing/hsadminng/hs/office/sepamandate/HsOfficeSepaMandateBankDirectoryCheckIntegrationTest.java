package net.hostsharing.hsadminng.hs.office.sepamandate;

import io.hypersistence.utils.hibernate.type.range.Range;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountIssues;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountRepository;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankDirectoryImporter;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankRepository;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.test.RbacDataJpaTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.time.LocalDate;

import static net.hostsharing.hsadminng.hs.office.bankaccount.TestHsOfficeBankAccount.hsOfficeBankAccount;
import static org.assertj.core.api.Assertions.assertThat;

@RbacDataJpaTest
@Tag("officeIntegrationTest")
@ExtendWith(OutputCaptureExtension.class)
class HsOfficeSepaMandateBankDirectoryCheckIntegrationTest extends ContextBasedTest {

    @Autowired
    HsOfficeBankAccountRepository bankAccountRepo;

    @Autowired
    HsOfficeDebitorRepository debitorRepo;

    @Autowired
    HsOfficeSepaMandateRepository sepaMandateRepo;

    @Autowired
    HsOfficeSepaMandateRealRepository sepaMandateRealRepo;

    @Autowired
    HsOfficeBankRepository bankRepo;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void logsTheNumberOfValidSepaMandatesWithMismatchingBankAccountButNoAccountData(final CapturedOutput output) {
        // given
        context("hsh-alex_superuser");
        jdbc.update("insert into hs_office.bank (countryCode, bankCode, bic, source) values ('DE', '98010000', 'BSPNDEB0XXX', 'MANUAL')");
        givenBankAccountWithSepaMandate("check test - matching", "DE00 9801 0000 0000 0000 01", "bspndeb0");
        givenBankAccountWithSepaMandate("check test - wrong BIC", "DE00980100000000000002", "TYPODEB0");
        givenBankAccountWithSepaMandate("check test - unknown bank", "DE00980300000000000001", "GLSCDEB0XXX");
        givenBankAccountWithSepaMandate("check test - foreign", "AT00980500000000000001", "BSPOATW0");
        givenBankAccountWithSepaMandate("check test - expired", "DE00980100000000000003", "TYPODEB0",
                Range.closedOpen(LocalDate.parse("2024-01-01"), LocalDate.parse("2025-01-01")));
        givenBankAccountWithSepaMandate("check test - never valid", "DE00980100000000000005", "TYPODEB0",
                Range.emptyRange(LocalDate.class));
        bankAccountRepo.save(hsOfficeBankAccount("check test - unreferenced", "DE00980100000000000004", "TYPODEB0"));
        em.flush();

        // when
        new HsOfficeSepaMandateBankDirectoryCheck(sepaMandateRealRepo, new HsOfficeBankAccountIssues(bankRepo))
                .logNumberOfMismatchingSepaMandates(new HsOfficeBankDirectoryImporter.Imported(URI.create("https://bank-directory.example.org/v1.csv")));

        // then
        assertThat(output)
                .contains("2 valid SEPA mandates have a bank account which does not match the bank directory")
                .doesNotContain("DE00980100000000000002")
                .doesNotContain("check test");
    }

    private void givenBankAccountWithSepaMandate(final String holder, final String iban, final String bic) {
        givenBankAccountWithSepaMandate(holder, iban, bic, Range.closedInfinite(LocalDate.parse("2024-01-01")));
    }

    private void givenBankAccountWithSepaMandate(
            final String holder, final String iban, final String bic, final Range<LocalDate> validity) {
        sepaMandateRepo.save(HsOfficeSepaMandateEntity.builder()
                .debitor(debitorRepo.findDebitorsByOptionalNameLike("First").getFirst())
                .bankAccount(bankAccountRepo.save(hsOfficeBankAccount(holder, iban, bic)))
                .reference(holder)
                .agreement(LocalDate.parse("2024-01-01"))
                .validity(validity)
                .build());
    }
}
