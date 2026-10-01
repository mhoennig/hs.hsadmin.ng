package net.hostsharing.hsadminng.hs.office.sepamandate;

import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountIssues;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankDirectoryImporter;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankEntity;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class HsOfficeSepaMandateBankDirectoryCheckUnitTest {

    private static final HsOfficeBankDirectoryImporter.Imported IMPORTED =
            new HsOfficeBankDirectoryImporter.Imported(URI.create("https://bank-directory.example.org/v1.csv"));

    final HsOfficeSepaMandateRealRepository sepaMandateRealRepo = mock(HsOfficeSepaMandateRealRepository.class);
    final HsOfficeBankRepository bankRepo = mock(HsOfficeBankRepository.class);
    final HsOfficeSepaMandateBankDirectoryCheck check =
            new HsOfficeSepaMandateBankDirectoryCheck(sepaMandateRealRepo, new HsOfficeBankAccountIssues(bankRepo));

    @Test
    void logsTheNumberOfMismatchingSepaMandatesButNoAccountData(final CapturedOutput output) {
        // given
        givenGermanBank("98010000", "BSPNDEB0XXX");
        givenBankAccountsOfValidSepaMandates(
                bankAccount("DE00 9801 0000 0000 0000 01", "bspndeb0"),
                bankAccount("DE00980100000000000002", "TYPODEB0"),
                bankAccount("DE00980300000000000001", "GLSCDEB0XXX"),
                bankAccount("AT00980500000000000001", "BSPOATW0"));

        // when
        check.logNumberOfMismatchingSepaMandates(IMPORTED);

        // then
        assertThat(output)
                .contains("2 valid SEPA mandates have a bank account which does not match the bank directory")
                .doesNotContain("DE00980100000000000002");
    }

    @Test
    void logsThatAllSepaMandatesMatch(final CapturedOutput output) {
        // given
        givenGermanBank("98010000", "BSPNDEB0XXX");
        givenBankAccountsOfValidSepaMandates(bankAccount("DE00980100000000000001", "BSPNDEB0"));

        // when
        check.logNumberOfMismatchingSepaMandates(IMPORTED);

        // then
        assertThat(output).contains("the bank accounts of all valid SEPA mandates match the bank directory");
    }

    private void givenGermanBank(final String bankCode, final String bic) {
        when(bankRepo.findAllByCountryCode("DE")).thenReturn(List.of(
                HsOfficeBankEntity.builder().countryCode("DE").bankCode(bankCode).bic(bic).build()));
    }

    private void givenBankAccountsOfValidSepaMandates(final HsOfficeSepaMandateRealRepository.BankAccountOfSepaMandate... bankAccounts) {
        when(sepaMandateRealRepo.findBankAccountsOfValidSepaMandates()).thenReturn(List.of(bankAccounts));
    }

    private static HsOfficeSepaMandateRealRepository.BankAccountOfSepaMandate bankAccount(final String iban, final String bic) {
        return new HsOfficeSepaMandateRealRepository.BankAccountOfSepaMandate() {

            @Override
            public String getIban() {
                return iban;
            }

            @Override
            public String getBic() {
                return bic;
            }
        };
    }
}
