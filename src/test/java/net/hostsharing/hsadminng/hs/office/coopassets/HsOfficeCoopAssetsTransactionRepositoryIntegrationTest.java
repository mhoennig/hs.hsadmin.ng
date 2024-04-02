package net.hostsharing.hsadminng.hs.office.coopassets;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipRepository;
import net.hostsharing.hsadminng.hs.office.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleRepository;
import net.hostsharing.test.Array;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
class HsOfficeCoopAssetsTransactionRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsOfficeCoopAssetsTransactionRepository coopAssetsTransactionRepo;

    @Autowired
    HsOfficeMembershipRepository membershipRepo;

    @Autowired
    RawRbacRoleRepository rawRoleRepo;

    @Autowired
    RawRbacGrantRepository rawGrantRepo;

    @PersistenceContext
    EntityManager em;

    @Autowired
    JpaAttempt jpaAttempt;

    @MockBean
    HttpServletRequest request;

    @Nested
    class CreateCoopAssetsTransaction {

        @Test
        public void globalAdmin_canCreateNewCoopAssetTransaction() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = coopAssetsTransactionRepo.count();
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000101);

            // when
            final var result = attempt(em, () -> {
                final var newCoopAssetsTransaction = HsOfficeCoopAssetsTransactionEntity.builder()
                        .membership(givenMembership)
                        .transactionType(HsOfficeCoopAssetsTransactionType.DEPOSIT)
                        .assetValue(new BigDecimal("128.00"))
                        .valueDate(LocalDate.parse("2022-10-18"))
                        .reference("temp ref A")
                        .build();
                return coopAssetsTransactionRepo.save(newCoopAssetsTransaction);
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeCoopAssetsTransactionEntity::getUuid).isNotNull();
            assertThatCoopAssetsTransactionIsPersisted(result.returnedValue());
            assertThat(coopAssetsTransactionRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll()).stream()
                    .map(s -> s.replace("hs_office_", ""))
                    .toList();

            // when
            attempt(em, () -> {
                final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000101);
                final var newCoopAssetsTransaction = HsOfficeCoopAssetsTransactionEntity.builder()
                        .membership(givenMembership)
                        .transactionType(HsOfficeCoopAssetsTransactionType.DEPOSIT)
                        .assetValue(new BigDecimal("128.00"))
                        .valueDate(LocalDate.parse("2022-10-18"))
                        .reference("temp ref B")
                        .build();
                return coopAssetsTransactionRepo.save(newCoopAssetsTransaction);
            });

            // then
            final var all = rawRoleRepo.findAll();
            assertThat(distinctRoleNamesOf(all)).containsExactlyInAnyOrder(Array.from(initialRoleNames)); // no new roles created
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll()))
                    .map(s -> s.replace("hs_office_", ""))
                    .containsExactlyInAnyOrder(Array.fromFormatted(
                            initialGrantNames,
                            "{ grant perm:coopassetstransaction#temprefB:SELECT to role:membership#M-1000101:AGENT by system and assume }",
                            "{ grant perm:coopassetstransaction#temprefB:UPDATE to role:membership#M-1000101:ADMIN by system and assume }",
                            null));
        }

        private void assertThatCoopAssetsTransactionIsPersisted(final HsOfficeCoopAssetsTransactionEntity saved) {
            final var found = coopAssetsTransactionRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class FindAllCoopAssetsTransactions {

        @Test
        public void globalAdmin_anViewAllCoopAssetsTransactions() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = coopAssetsTransactionRepo.findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange(
                    null,
                    null,
                    null);

            // then
            allTheseCoopAssetsTransactionsAreReturned(
                    result,
                    "CoopAssetsTransaction(M-1000101: 2010-03-15, DEPOSIT, 320.00, ref 1000101-1, initial deposit)",
                    "CoopAssetsTransaction(M-1000101: 2021-09-01, DISBURSAL, -128.00, ref 1000101-2, partial disbursal)",
                    "CoopAssetsTransaction(M-1000101: 2022-10-20, ADJUSTMENT, 128.00, ref 1000101-3, some adjustment)",

                    "CoopAssetsTransaction(M-1000202: 2010-03-15, DEPOSIT, 320.00, ref 1000202-1, initial deposit)",
                    "CoopAssetsTransaction(M-1000202: 2021-09-01, DISBURSAL, -128.00, ref 1000202-2, partial disbursal)",
                    "CoopAssetsTransaction(M-1000202: 2022-10-20, ADJUSTMENT, 128.00, ref 1000202-3, some adjustment)",

                    "CoopAssetsTransaction(M-1000303: 2010-03-15, DEPOSIT, 320.00, ref 1000303-1, initial deposit)",
                    "CoopAssetsTransaction(M-1000303: 2021-09-01, DISBURSAL, -128.00, ref 1000303-2, partial disbursal)",
                    "CoopAssetsTransaction(M-1000303: 2022-10-20, ADJUSTMENT, 128.00, ref 1000303-3, some adjustment)");
        }

        @Test
        public void globalAdmin_canViewCoopAssetsTransactions_filteredByMembershipUuid() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000202);

            // when
            final var result = coopAssetsTransactionRepo.findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange(
                    givenMembership.getUuid(),
                    null,
                    null);

            // then
            allTheseCoopAssetsTransactionsAreReturned(
                    result,
                    "CoopAssetsTransaction(M-1000202: 2010-03-15, DEPOSIT, 320.00, ref 1000202-1, initial deposit)",
                    "CoopAssetsTransaction(M-1000202: 2021-09-01, DISBURSAL, -128.00, ref 1000202-2, partial disbursal)",
                    "CoopAssetsTransaction(M-1000202: 2022-10-20, ADJUSTMENT, 128.00, ref 1000202-3, some adjustment)");
        }

        @Test
        public void globalAdmin_canViewCoopAssetsTransactions_filteredByMembershipUuidAndValueDateRange() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000202);

            // when
            final var result = coopAssetsTransactionRepo.findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange(
                    givenMembership.getUuid(),
                    LocalDate.parse("2021-09-01"),
                    LocalDate.parse("2021-09-01"));

            // then
            allTheseCoopAssetsTransactionsAreReturned(
                    result,
                    "CoopAssetsTransaction(M-1000202: 2021-09-01, DISBURSAL, -128.00, ref 1000202-2, partial disbursal)");
        }

        @Test
        public void partnerPersonAdmin_canViewRelatedCoopAssetsTransactions() {
            // given:
            context("superuser-alex@hostsharing.net", "hs_office_person#FirstGmbH:ADMIN");

            // when:
            final var result = coopAssetsTransactionRepo.findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange(
                    null,
                    null,
                    null);

            // then:
            exactlyTheseCoopAssetsTransactionsAreReturned(
                    result,
                    "CoopAssetsTransaction(M-1000101: 2010-03-15, DEPOSIT, 320.00, ref 1000101-1, initial deposit)",
                    "CoopAssetsTransaction(M-1000101: 2021-09-01, DISBURSAL, -128.00, ref 1000101-2, partial disbursal)",
                    "CoopAssetsTransaction(M-1000101: 2022-10-20, ADJUSTMENT, 128.00, ref 1000101-3, some adjustment)");
        }
    }

    @Test
    public void auditJournalLogIsAvailable() {
        // given
        final var query = em.createNativeQuery("""
                select currentTask, targetTable, targetOp
                    from tx_journal_v
                    where targettable = 'hs_office_coopassetstransaction';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating coopAssetsTransaction test-data 1000101, hs_office_coopassetstransaction, INSERT]",
                "[creating coopAssetsTransaction test-data 1000202, hs_office_coopassetstransaction, INSERT]");
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net", null);
            em.createQuery("DELETE FROM HsOfficeCoopAssetsTransactionEntity WHERE reference like 'temp ref%'");
        });
    }

    void exactlyTheseCoopAssetsTransactionsAreReturned(
            final List<HsOfficeCoopAssetsTransactionEntity> actualResult,
            final String... coopAssetsTransactionNames) {
        assertThat(actualResult)
                .extracting(coopAssetsTransactionEntity -> coopAssetsTransactionEntity.toString())
                .containsExactlyInAnyOrder(coopAssetsTransactionNames);
    }

    void allTheseCoopAssetsTransactionsAreReturned(
            final List<HsOfficeCoopAssetsTransactionEntity> actualResult,
            final String... coopAssetsTransactionNames) {
        assertThat(actualResult)
                .extracting(coopAssetsTransactionEntity -> coopAssetsTransactionEntity.toString())
                .contains(coopAssetsTransactionNames);
    }
}
