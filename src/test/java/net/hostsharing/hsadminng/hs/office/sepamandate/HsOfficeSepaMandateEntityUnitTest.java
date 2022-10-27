package net.hostsharing.hsadminng.hs.office.sepamandate;

import com.vladmihalcea.hibernate.type.range.Range;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static net.hostsharing.hsadminng.hs.office.debitor.TestHsOfficeDebitor.TEST_DEBITOR;
import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeSepaMandateEntityUnitTest {

    final HsOfficeSepaMandateEntity givenSepaMandate = HsOfficeSepaMandateEntity.builder()
            .debitor(TEST_DEBITOR)
            .reference("some-ref")
            .validity(Range.closedOpen(LocalDate.parse("2020-01-01"), LocalDate.parse("2031-01-01")))
            .bankAccount(HsOfficeBankAccountEntity.builder().iban("some label").build())
            .build();

    @Test
    void toStringContainsReferenceAndBankAccount() {
        final var result = givenSepaMandate.toString();

        assertThat(result).isEqualTo("SEPA-Mandate(some label, some-ref, [2020-01-01,2031-01-01))");
    }

    @Test
    void toShortStringContainsReferenceOnly() {
        final var result = givenSepaMandate.toShortString();

        assertThat(result).isEqualTo("some-ref");
    }
}
