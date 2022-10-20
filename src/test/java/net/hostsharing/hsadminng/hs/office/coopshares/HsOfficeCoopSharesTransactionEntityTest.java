package net.hostsharing.hsadminng.hs.office.coopshares;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static net.hostsharing.hsadminng.hs.office.membership.TestHsMembership.testMembership;
import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeCoopSharesTransactionEntityTest {

    final HsOfficeCoopSharesTransactionEntity givenCoopSharesTransaction = HsOfficeCoopSharesTransactionEntity.builder()
            .membership(testMembership)
            .reference("some-ref")
            .valueDate(LocalDate.parse("2020-01-01"))
            .transactionType(HsOfficeCoopSharesTransactionType.SUBSCRIPTION)
            .shareCount(4)
            .build();

    @Test
    void toStringContainsAlmostAllPropertiesAccount() {
        final var result = givenCoopSharesTransaction.toString();

        assertThat(result).isEqualTo("CoopShareTransaction(300001, 2020-01-01, SUBSCRIPTION, 4, some-ref)");
    }

    @Test
    void toShortStringContainsOnlyMemberNumberAndshareCountOnly() {
        final var result = givenCoopSharesTransaction.toShortString();

        assertThat(result).isEqualTo("300001+4");
    }
}
