package net.hostsharing.hsadminng.hs.office.coopassets;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipRepository;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.grantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.roleNamesOf;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
class HsOfficeCoopAssetsTransactionRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    HsOfficeCoopAssetsTransactionRepository coopAssetsTransactionRepo;

    @Autowired
    HsOfficeMembershipRepository membershipRepo;

    @Autowired
    RawRbacRoleRepository rawRoleRepo;

    @Autowired
    RawRbacGrantRepository rawGrantRepo;

    @Autowired
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
            final var givenMembership = membershipRepo.findMembershipsByOptionalPartnerUuidAndOptionalMemberNumber(null, 10001)
                    .get(0);

            // when
            final var result = attempt(em, () -> {
                final var newCoopAssetsTransaction = HsOfficeCoopAssetsTransactionEntity.builder()
                        .uuid(UUID.randomUUID())
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
            final var initialRoleNames = roleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = grantDisplaysOf(rawGrantRepo.findAll()).stream()
                    .map(s -> s.replace("FirstGmbH-firstcontact", "..."))
                    .map(s -> s.replace("hs_office_", ""))
                    .toList();

            // when
            attempt(em, () -> {
                final var givenMembership = membershipRepo.findMembershipsByOptionalPartnerUuidAndOptionalMemberNumber(
                        null,
                        10001).get(0);
                final var newCoopAssetsTransaction = HsOfficeCoopAssetsTransactionEntity.builder()
                        .uuid(UUID.randomUUID())
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
            assertThat(roleNamesOf(all)).containsExactlyInAnyOrder(Array.from(initialRoleNames)); // no new roles created
            assertThat(grantDisplaysOf(rawGrantRepo.findAll()))
                    .map(s -> s.replace("FirstGmbH-firstcontact", "..."))
                    .map(s -> s.replace("hs_office_", ""))
                    .containsExactlyInAnyOrder(Array.fromFormatted(
                            initialGrantNames,
                            "{ grant perm view on coopassetstransaction#temprefB to role membership#10001....tenant by system and assume }",
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
                    "CoopAssetsTransaction(10001, 2010-03-15, DEPOSIT, 320.00, ref 10001-1)",
                    "CoopAssetsTransaction(10001, 2021-09-01, DISBURSAL, -128.00, ref 10001-2)",
                    "CoopAssetsTransaction(10001, 2022-10-20, ADJUSTMENT, 128.00, ref 10001-3)",

                    "CoopAssetsTransaction(10002, 2010-03-15, DEPOSIT, 320.00, ref 10002-1)",
                    "CoopAssetsTransaction(10002, 2021-09-01, DISBURSAL, -128.00, ref 10002-2)",
                    "CoopAssetsTransaction(10002, 2022-10-20, ADJUSTMENT, 128.00, ref 10002-3)",

                    "CoopAssetsTransaction(10003, 2010-03-15, DEPOSIT, 320.00, ref 10003-1)",
                    "CoopAssetsTransaction(10003, 2021-09-01, DISBURSAL, -128.00, ref 10003-2)",
                    "CoopAssetsTransaction(10003, 2022-10-20, ADJUSTMENT, 128.00, ref 10003-3)");
        }

        @Test
        public void globalAdmin_canViewCoopAssetsTransactions_filteredByMembershipUuid() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipsByOptionalPartnerUuidAndOptionalMemberNumber(null, 10002)
                    .get(0);

            // when
            final var result = coopAssetsTransactionRepo.findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange(
                    givenMembership.getUuid(),
                    null,
                    null);

            // then
            allTheseCoopAssetsTransactionsAreReturned(
                    result,
                    "CoopAssetsTransaction(10002, 2010-03-15, DEPOSIT, 320.00, ref 10002-1)",
                    "CoopAssetsTransaction(10002, 2021-09-01, DISBURSAL, -128.00, ref 10002-2)",
                    "CoopAssetsTransaction(10002, 2022-10-20, ADJUSTMENT, 128.00, ref 10002-3)");
        }

        @Test
        public void globalAdmin_canViewCoopAssetsTransactions_filteredByMembershipUuidAndValueDateRange() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipsByOptionalPartnerUuidAndOptionalMemberNumber(null, 10002)
                    .get(0);

            // when
            final var result = coopAssetsTransactionRepo.findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange(
                    givenMembership.getUuid(),
                    LocalDate.parse("2021-09-01"),
                    LocalDate.parse("2021-09-01"));

            // then
            allTheseCoopAssetsTransactionsAreReturned(
                    result,
                    "CoopAssetsTransaction(10002, 2021-09-01, DISBURSAL, -128.00, ref 10002-2)");
        }

        @Test
        public void normalUser_canViewOnlyRelatedCoopAssetsTransactions() {
            // given:
            context("superuser-alex@hostsharing.net", "hs_office_partner#FirstGmbH-firstcontact.admin");
            //                    "hs_office_person#FirstGmbH.admin",

            // when:
            final var result = coopAssetsTransactionRepo.findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange(
                    null,
                    null,
                    null);

            // then:
            exactlyTheseCoopAssetsTransactionsAreReturned(
                    result,
                    "CoopAssetsTransaction(10001, 2010-03-15, DEPOSIT, 320.00, ref 10001-1)",
                    "CoopAssetsTransaction(10001, 2021-09-01, DISBURSAL, -128.00, ref 10001-2)",
                    "CoopAssetsTransaction(10001, 2022-10-20, ADJUSTMENT, 128.00, ref 10001-3)");
        }
    }

    @Test
    public void auditJournalLogIsAvailable() {
        // given
        final var query = em.createNativeQuery("""
                select c.currenttask, j.targettable, j.targetop
                    from tx_journal j
                    join tx_context c on j.contextId = c.contextId
                    where targettable = 'hs_office_coopassetstransaction';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating coopAssetsTransaction test-data 10001, hs_office_coopassetstransaction, INSERT]",
                "[creating coopAssetsTransaction test-data 10002, hs_office_coopassetstransaction, INSERT]");
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
