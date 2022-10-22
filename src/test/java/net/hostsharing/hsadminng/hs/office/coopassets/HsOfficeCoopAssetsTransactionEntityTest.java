package net.hostsharing.hsadminng.hs.office.coopassets;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static net.hostsharing.hsadminng.hs.office.membership.TestHsMembership.TEST_MEMBERSHIP;
import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeCoopAssetsTransactionEntityTest {

    final HsOfficeCoopAssetsTransactionEntity givenCoopAssetTransaction = HsOfficeCoopAssetsTransactionEntity.builder()
            .membership(TEST_MEMBERSHIP)
            .reference("some-ref")
            .valueDate(LocalDate.parse("2020-01-01"))
            .transactionType(HsOfficeCoopAssetsTransactionType.DEPOSIT)
            .assetValue(new BigDecimal("128.00"))
            .build();

    @Test
    void toStringContainsAlmostAllPropertiesAccount() {
        final var result = givenCoopAssetTransaction.toString();

        assertThat(result).isEqualTo("CoopAssetsTransaction(300001, 2020-01-01, DEPOSIT, 128.00, some-ref)");
    }

    @Test
    void toShortStringContainsOnlyMemberNumberAndSharesCountOnly() {
        final var result = givenCoopAssetTransaction.toShortString();

        assertThat(result).isEqualTo("300001+128.00");
    }
}
