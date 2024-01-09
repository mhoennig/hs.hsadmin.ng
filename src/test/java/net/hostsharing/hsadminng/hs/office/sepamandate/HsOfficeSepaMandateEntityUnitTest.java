package net.hostsharing.hsadminng.hs.office.sepamandate;

import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static net.hostsharing.hsadminng.hs.office.debitor.TestHsOfficeDebitor.TEST_DEBITOR;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.toPostgresDateRange;
import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeSepaMandateEntityUnitTest {
    public static final LocalDate GIVEN_VALID_FROM = LocalDate.parse("2020-01-01");
    public static final LocalDate GIVEN_VALID_TO = LocalDate.parse("2030-12-31");

    final HsOfficeSepaMandateEntity givenSepaMandate = HsOfficeSepaMandateEntity.builder()
            .debitor(TEST_DEBITOR)
            .reference("some-ref")
            .validity(toPostgresDateRange(GIVEN_VALID_FROM, GIVEN_VALID_TO))
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

    @Test
    void settingValidFromKeepsValidTo() {
        givenSepaMandate.setValidFrom(LocalDate.parse("2023-12-31"));
        assertThat(givenSepaMandate.getValidFrom()).isEqualTo(LocalDate.parse("2023-12-31"));
        assertThat(givenSepaMandate.getValidTo()).isEqualTo(GIVEN_VALID_TO);

    }

    @Test
    void settingValidToKeepsValidFrom() {
        givenSepaMandate.setValidTo(LocalDate.parse("2024-12-31"));
        assertThat(givenSepaMandate.getValidFrom()).isEqualTo(GIVEN_VALID_FROM);
        assertThat(givenSepaMandate.getValidTo()).isEqualTo(LocalDate.parse("2024-12-31"));
    }

}
