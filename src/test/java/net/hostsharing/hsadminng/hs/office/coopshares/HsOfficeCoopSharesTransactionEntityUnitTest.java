package net.hostsharing.hsadminng.hs.office.coopshares;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static net.hostsharing.hsadminng.hs.office.membership.TestHsMembership.TEST_MEMBERSHIP;
import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeCoopSharesTransactionEntityUnitTest {

    final HsOfficeCoopSharesTransactionEntity givenCoopSharesTransaction = HsOfficeCoopSharesTransactionEntity.builder()
            .membership(TEST_MEMBERSHIP)
            .reference("some-ref")
            .valueDate(LocalDate.parse("2020-01-01"))
            .transactionType(HsOfficeCoopSharesTransactionType.SUBSCRIPTION)
            .shareCount(4)
            .comment("some comment")
            .build();


    final HsOfficeCoopSharesTransactionEntity givenCoopShareAdjustmentTransaction = HsOfficeCoopSharesTransactionEntity.builder()
        .membership(TEST_MEMBERSHIP)
        .reference("some-ref")
        .valueDate(LocalDate.parse("2020-01-15"))
        .transactionType(HsOfficeCoopSharesTransactionType.ADJUSTMENT)
        .shareCount(-4)
        .comment("some comment")
        .adjustedShareTx(givenCoopSharesTransaction)
        .build();

    final HsOfficeCoopSharesTransactionEntity givenEmptyCoopSharesTransaction = HsOfficeCoopSharesTransactionEntity.builder().build();

    @Test
    void toStringContainsAllNonNullProperties() {
        final var result = givenCoopSharesTransaction.toString();

        assertThat(result).isEqualTo("CoopShareTransaction(M-1000101: 2020-01-01, SUBSCRIPTION, 4, some-ref, some comment)");
    }

    @Test
    void toStringWithReverseEntryContainsReverseEntry() {
        givenCoopSharesTransaction.setAdjustedShareTx(givenCoopShareAdjustmentTransaction);

        final var result = givenCoopSharesTransaction.toString();

        assertThat(result).isEqualTo("CoopShareTransaction(M-1000101: 2020-01-01, SUBSCRIPTION, 4, some-ref, some comment, M-1000101:ADJ:-4)");
    }

    @Test
    void toShortStringContainsOnlyAbbreviatedString() {
        final var result = givenCoopSharesTransaction.toShortString();

        assertThat(result).isEqualTo("M-1000101:SUB:+4");
    }

    @Test
    void toStringEmptyTransactionDoesNotThrowException() {
        final var result = givenEmptyCoopSharesTransaction.toString();

        assertThat(result).isEqualTo("CoopShareTransaction(null: 0)");
    }

    @Test
    void toShortStringEmptyTransactionDoesNotThrowException() {
        final var result = givenEmptyCoopSharesTransaction.toShortString();

        assertThat(result).isEqualTo("null:nul:+0");
    }
}
