package net.hostsharing.hsadminng.hs.office.bankaccount;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeBankAccountEntityUnitTest {

    @Test
    void toStringReturnsNullForNullBankAccount() {
        final HsOfficeBankAccountEntity givenBankAccount = null;
        assertThat("" + givenBankAccount).isEqualTo("null");
    }

    @Test
    void toStringReturnsAllProperties() {
        final var givenBankAccount = HsOfficeBankAccountEntity.builder()
                .holder("given holder")
                .iban("DE02370502990000684712")
                .bic("COKSDE33")
                .build();
        assertThat("" + givenBankAccount).isEqualTo("bankAccount(holder='given holder', iban='DE02370502990000684712', bic='COKSDE33')");
    }

    @Test
    void toShotStringReturnsHolder() {
        final var givenBankAccount = HsOfficeBankAccountEntity.builder()
                .holder("given holder")
                .iban("DE02370502990000684712")
                .bic("COKSDE33")
                .build();
        assertThat(givenBankAccount.toShortString()).isEqualTo("given holder");
    }

}
