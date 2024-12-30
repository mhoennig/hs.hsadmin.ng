package net.hostsharing.hsadminng.hs.office.bankaccount;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.grant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.role.RawRbacRoleRepository;
import net.hostsharing.hsadminng.mapper.Array;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static net.hostsharing.hsadminng.hs.office.bankaccount.TestHsOfficeBankAccount.hsOfficeBankAccount;
import static net.hostsharing.hsadminng.rbac.grant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.role.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ Context.class, JpaAttempt.class })
class HsOfficeBankAccountRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsOfficeBankAccountRepository bankAccountRepo;

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
    class CreateBankAccount {

        @Test
        public void globalAdmin_canCreateNewBankAccount() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = bankAccountRepo.count();

            // when
            final var result = attempt(em, () -> toCleanup(bankAccountRepo.save(
                    hsOfficeBankAccount("some temp acc A", "DE37500105177419788228", ""))));

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeBankAccountEntity::getUuid).isNotNull();
            assertThatBankAccountIsPersisted(result.returnedValue());
            assertThat(bankAccountRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void arbitraryUser_canCreateNewBankAccount() {
            // given
            context("selfregistered-user-drew@hostsharing.org");
            final var count = bankAccountRepo.count();

            // when
            final var result = attempt(em, () -> toCleanup(bankAccountRepo.save(
                    hsOfficeBankAccount("some temp acc B", "DE49500105174516484892", "INGDDEFFXXX"))));

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeBankAccountEntity::getUuid).isNotNull();
            assertThatBankAccountIsPersisted(result.returnedValue());
            assertThat(bankAccountRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("selfregistered-user-drew@hostsharing.org");
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll());

            // when
            attempt(em, () -> toCleanup(bankAccountRepo.save(
                    hsOfficeBankAccount("some temp acc C", "DE25500105176934832579", "INGDDEFFXXX")))
            ).assertSuccessful();

            // then
            final var roles = rawRoleRepo.findAll();
            assertThat(distinctRoleNamesOf(roles)).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_office.bankaccount#DE25500105176934832579:OWNER",
                    "hs_office.bankaccount#DE25500105176934832579:ADMIN",
                    "hs_office.bankaccount#DE25500105176934832579:REFERRER"
            ));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(Array.fromFormatted(
                    initialGrantNames,
                    "{ grant perm:hs_office.bankaccount#DE25500105176934832579:DELETE   to role:hs_office.bankaccount#DE25500105176934832579:OWNER     by system and assume }",
                    "{ grant role:hs_office.bankaccount#DE25500105176934832579:OWNER    to role:rbac.global#global:ADMIN                                    by system and assume }",
                    "{ grant role:hs_office.bankaccount#DE25500105176934832579:OWNER    to user:selfregistered-user-drew@hostsharing.org               by hs_office.bankaccount#DE25500105176934832579:OWNER and assume }",

                    "{ grant role:hs_office.bankaccount#DE25500105176934832579:ADMIN    to role:hs_office.bankaccount#DE25500105176934832579:OWNER     by system and assume }",
                    "{ grant perm:hs_office.bankaccount#DE25500105176934832579:UPDATE   to role:hs_office.bankaccount#DE25500105176934832579:ADMIN     by system and assume }",

                    "{ grant perm:hs_office.bankaccount#DE25500105176934832579:SELECT   to role:hs_office.bankaccount#DE25500105176934832579:REFERRER  by system and assume }",
                    "{ grant role:hs_office.bankaccount#DE25500105176934832579:REFERRER to role:hs_office.bankaccount#DE25500105176934832579:ADMIN     by system and assume }",
                    null
            ));
        }

        private void assertThatBankAccountIsPersisted(final HsOfficeBankAccountEntity saved) {
            final var found = bankAccountRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().extracting(Object::toString).isEqualTo(saved.toString());
        }
    }

    @Nested
    class GetListOfBankAccounts {

        @Test
        public void globalAdmin_canViewAllBankAccounts() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = bankAccountRepo.findByOptionalHolderLike(null);

            // then
            allTheseBankAccountsAreReturned(
                    result,
                    "Anita Bessler",
                    "First GmbH",
                    "Fourth eG",
                    "Mel Bessler",
                    "Paul Winkler",
                    "Peter Smith",
                    "Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K.",
                    "Third OHG");
        }

        @Test
        public void arbitraryUser_canViewOnlyItsOwnBankAccount() {
            // given:
            final var givenBankAccount = givenSomeTemporaryBankAccount("selfregistered-user-drew@hostsharing.org");

            // when:
            context("selfregistered-user-drew@hostsharing.org");
            final var result = bankAccountRepo.findByOptionalHolderLike(null);

            // then:
            exactlyTheseBankAccountsAreReturned(result, givenBankAccount.getHolder());
        }

        @Test
        public void globalAdmin_canViewBankAccountsByIban() {
            // given
            context("superuser-alex@hostsharing.net", null);

            // when
            final var result = bankAccountRepo.findByIbanOrderByIbanAsc("DE02120300000000202051");

            // then
            exactlyTheseBankAccountsAreReturned(result, "First GmbH");
        }

        @Test
        public void arbitraryUser_canViewItsOwnBankAccount() {
            // given:
            final var givenBankAccount = givenSomeTemporaryBankAccount("selfregistered-user-drew@hostsharing.org");

            // when:
            context("selfregistered-user-drew@hostsharing.org");
            final var result = bankAccountRepo.findByIbanOrderByIbanAsc(givenBankAccount.getIban());

            // then:
            exactlyTheseBankAccountsAreReturned(result, givenBankAccount.getHolder());
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_canDeleteAnyBankAccount() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenBankAccount = givenSomeTemporaryBankAccount("selfregistered-user-drew@hostsharing.org");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                bankAccountRepo.deleteByUuid(givenBankAccount.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                return bankAccountRepo.findByOptionalHolderLike(givenBankAccount.getHolder());
            }).assertSuccessful().returnedValue()).hasSize(0);
        }

        @Test
        public void arbitraryUser_canDeleteABankAccountCreatedByItself() {
            // given
            final var givenBankAccount = givenSomeTemporaryBankAccount("selfregistered-user-drew@hostsharing.org");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("selfregistered-user-drew@hostsharing.org", null);
                bankAccountRepo.deleteByUuid(givenBankAccount.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                return bankAccountRepo.findByOptionalHolderLike(givenBankAccount.getHolder());
            }).assertSuccessful().returnedValue()).hasSize(0);
        }

        @Test
        public void deletingABankAccountAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("selfregistered-user-drew@hostsharing.org", null);
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll());
            final var givenBankAccount = givenSomeTemporaryBankAccount("selfregistered-user-drew@hostsharing.org");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("selfregistered-user-drew@hostsharing.org", null);
                return bankAccountRepo.deleteByUuid(givenBankAccount.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(distinctRoleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames
            ));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialGrantNames
            ));
        }
    }

    private HsOfficeBankAccountEntity givenSomeTemporaryBankAccount(
            final String createdByUser,
            Supplier<HsOfficeBankAccountEntity> entitySupplier) {
        return jpaAttempt.transacted(() -> {
            context(createdByUser);
            return toCleanup(bankAccountRepo.save(entitySupplier.get()));
        }).assertSuccessful().returnedValue();
    }

    @Test
    public void auditJournalLogIsAvailable() {
        // given
        final var query = em.createNativeQuery("""
                select currentTask, targetTable, targetOp, targetdelta->>'iban'
                    from base.tx_journal_v
                    where targettable = 'hs_office.bankaccount';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating bankaccount test-data, hs_office.bankaccount, INSERT, DE02120300000000202051]",
                "[creating bankaccount test-data, hs_office.bankaccount, INSERT, DE02500105170137075030]",
                "[creating bankaccount test-data, hs_office.bankaccount, INSERT, DE02100500000054540402]");
    }

    private HsOfficeBankAccountEntity givenSomeTemporaryBankAccount(final String createdByUser) {
        final var random = RandomStringUtils.randomAlphabetic(3);
        return givenSomeTemporaryBankAccount(createdByUser, () ->
                hsOfficeBankAccount(
                        "some temp acc #" + random,
                        "DE41500105177739718697",
                        "INGDDEFFXXX"
                ));
    }

    void exactlyTheseBankAccountsAreReturned(
            final List<HsOfficeBankAccountEntity> actualResult,
            final String... bankaccountCaptions) {
        assertThat(actualResult)
                .extracting(HsOfficeBankAccountEntity::getHolder)
                .containsExactlyInAnyOrder(bankaccountCaptions);
    }

    void allTheseBankAccountsAreReturned(
            final List<HsOfficeBankAccountEntity> actualResult,
            final String... bankaccountCaptions) {
        assertThat(actualResult)
                .extracting(HsOfficeBankAccountEntity::getHolder)
                .contains(bankaccountCaptions);
    }
}
