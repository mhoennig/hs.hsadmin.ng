package net.hostsharing.hsadminng.hs.office.bankaccount;

import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeBankEntityUnitTest {

    @ParameterizedTest
    @CsvSource({
            "DEMDDEB0,    DEMDDEB0,    true",
            "DEMDDEB0,    DEMDDEB0XXX, true",
            "DEMDDEB0XXX, DEMDDEB0,    true",
            "DEMDDEB0XXX, DEMDDEB0XXX, true",
            "DEMDDEB0,    DEMDDEB0ABC, false",
            "DEMDDEB0ABC, DEMDDEB0XXX, false",
            "DEMDDEB0,    DEMDDEB1,    false",
            "DEMXXX,      DEMXXX,      true",
            "DEMXXX,      DEMDDEB0,    false"
    })
    void hasBicIgnoresTheXxxBranchCodeSuffix(final String directoryBic, final String givenBic, final boolean expected) {
        val bank = HsOfficeBankEntity.builder().bic(directoryBic).build();

        assertThat(bank.hasBic(givenBic)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "DEMDDEB0", "DEMXXX" })
    void hasNoBicIfDirectoryHasNoBic(final String givenBic) {
        val bank = HsOfficeBankEntity.builder().build();

        assertThat(bank.hasBic(givenBic)).isFalse();
    }
}
