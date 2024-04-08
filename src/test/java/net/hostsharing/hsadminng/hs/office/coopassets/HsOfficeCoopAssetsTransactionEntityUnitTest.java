package net.hostsharing.hsadminng.hs.office.coopassets;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static net.hostsharing.hsadminng.hs.office.membership.TestHsMembership.TEST_MEMBERSHIP;
import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeCoopAssetsTransactionEntityUnitTest {

    final HsOfficeCoopAssetsTransactionEntity givenCoopAssetTransaction = HsOfficeCoopAssetsTransactionEntity.builder()
            .membership(TEST_MEMBERSHIP)
            .reference("some-ref")
            .valueDate(LocalDate.parse("2020-01-01"))
            .transactionType(HsOfficeCoopAssetsTransactionType.DEPOSIT)
            .assetValue(new BigDecimal("128.00"))
            .build();
    final HsOfficeCoopAssetsTransactionEntity givenEmptyCoopAssetsTransaction = HsOfficeCoopAssetsTransactionEntity.builder().build();

    @Test
    void toStringContainsAlmostAllPropertiesAccount() {
        final var result = givenCoopAssetTransaction.toString();

        assertThat(result).isEqualTo("CoopAssetsTransaction(M-1000101: 2020-01-01, DEPOSIT, 128.00, some-ref)");
    }

    @Test
    void toShortStringContainsOnlyMemberNumberSuffixAndSharesCountOnly() {
        final var result = givenCoopAssetTransaction.toShortString();

        assertThat(result).isEqualTo("M-1000101:+128.00");
    }

    @Test
    void toStringWithEmptyTransactionDoesNotThrowException() {
        final var result = givenEmptyCoopAssetsTransaction.toString();

        assertThat(result).isEqualTo("CoopAssetsTransaction(M-?????: )");
    }

    @Test
    void toShortStringEmptyTransactionDoesNotThrowException() {
        final var result = givenEmptyCoopAssetsTransaction.toShortString();

        assertThat(result).isEqualTo("M-?????:+0.00");
    }
}
